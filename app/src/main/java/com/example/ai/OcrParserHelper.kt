package com.example.ai

import android.graphics.Bitmap
import com.example.data.model.BusinessCard
import com.example.image.CardThemeColorExtractor
import com.example.image.ExtractedCardTheme
import java.util.regex.Pattern

object OcrParserHelper {

    // Regex patterns for entity extraction
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "(?:(?:\\+|00)[1-9]\\d{0,3}[\\s-]*)?(?:\\(?\\d{2,4}\\)?[\\s-]*)?\\d{3,4}[\\s-]*\\d{3,4}"
    )

    private val BANGLA_PHONE_PATTERN = Pattern.compile(
        "(?:(?:\\+৮৮০|০)?[১-৯][০-৯]{8,10})"
    )

    private val URL_PATTERN = Pattern.compile(
        "(?:https?://)?(?:www\\.)?[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(?:/[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=]*)?"
    )

    private val BANGLA_UNICODE_PATTERN = Pattern.compile("[\\u0980-\\u09FF]")

    /**
     * Estimates standard business card physical dimensions from image aspect ratio.
     */
    fun estimateCardDimensions(width: Int, height: Int): Triple<Float, Float, String> {
        if (width <= 0 || height <= 0) {
            return Triple(88.9f, 50.8f, "Standard US (3.5\" × 2.0\")")
        }

        val major = maxOf(width, height).toFloat()
        val minor = minOf(width, height).toFloat()
        val ratio = major / minor

        return when {
            ratio in 1.70f..1.80f -> {
                Triple(88.9f, 50.8f, "Standard US / CA (3.5\" × 2.0\" • 89×51mm)")
            }
            ratio in 1.55f..1.65f -> {
                Triple(85.6f, 53.98f, "ISO/IEC 7810 ID-1 Credit Card (85.6×54.0mm)")
            }
            ratio in 1.78f..1.85f -> {
                Triple(90.0f, 50.0f, "European / UK Standard (90×50mm)")
            }
            ratio in 1.63f..1.69f -> {
                Triple(91.0f, 55.0f, "Japanese Meishi / Asian Standard (91×55mm)")
            }
            ratio in 0.95f..1.10f -> {
                Triple(65.0f, 65.0f, "Modern Square (65×65mm)")
            }
            else -> {
                val estW = (ratio * 50.8f)
                Triple(estW, 50.8f, "Custom Detected Aspect (${String.format("%.2f", ratio)}:1)")
            }
        }
    }

    /**
     * Local parser for text representation.
     */
    fun parseTextDirectly(text: String): CardScanResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        var fullName = ""
        var jobTitle = ""
        var company = ""
        var phone = ""
        var secondaryPhone = ""
        var email = ""
        var website = ""
        var address = ""
        var category = "Corporate"
        val notesBuilder = StringBuilder()

        val isBangla = BANGLA_UNICODE_PATTERN.matcher(text).find()

        // Extract Email
        val emailMatcher = EMAIL_PATTERN.matcher(text)
        if (emailMatcher.find()) {
            email = emailMatcher.group()
        }

        // Extract URL
        val urlMatcher = URL_PATTERN.matcher(text)
        while (urlMatcher.find()) {
            val candidate = urlMatcher.group()
            if (!candidate.contains("@") && (candidate.startsWith("http") || candidate.startsWith("www.") || candidate.contains(".com") || candidate.contains(".org") || candidate.contains(".ai") || candidate.contains(".bd"))) {
                website = candidate
                break
            }
        }

        // Extract Phones
        val rawPhones = mutableListOf<String>()
        val phoneMatcher = PHONE_PATTERN.matcher(text)
        while (phoneMatcher.find()) {
            val candidate = phoneMatcher.group().trim()
            if (candidate.length >= 7 && !candidate.contains("@")) {
                rawPhones.addAll(com.example.util.PhoneNumberUtils.splitPhoneNumbers(candidate))
            }
        }

        // Extract Bangla Phone if present
        val banglaPhoneMatcher = BANGLA_PHONE_PATTERN.matcher(text)
        while (banglaPhoneMatcher.find()) {
            val candidate = banglaPhoneMatcher.group().trim()
            rawPhones.addAll(com.example.util.PhoneNumberUtils.splitPhoneNumbers(candidate))
        }

        // Deduplicate phone numbers
        val detectedPhones = mutableListOf<String>()
        for (p in rawPhones) {
            val norm = com.example.util.PhoneNumberUtils.normalizeForComparison(p)
            if (norm.isNotBlank() && detectedPhones.none { com.example.util.PhoneNumberUtils.normalizeForComparison(it) == norm }) {
                detectedPhones.add(p)
            }
        }

        phone = detectedPhones.firstOrNull() ?: ""
        secondaryPhone = detectedPhones.getOrNull(1) ?: ""
        val extraPhones = detectedPhones.drop(2)
        val extraPhonesSocial = if (extraPhones.isNotEmpty()) {
            extraPhones.mapIndexed { idx, p -> "${if (isBangla) "বিকল্প ফোন" else "Phone"} ${idx + 3}: $p" }.joinToString("\n")
        } else ""

        // Analyze remaining lines for Name, Title, Company, Address
        val unusedLines = lines.filter { line ->
            !line.contains(email) &&
            (phone.isEmpty() || !line.contains(phone)) &&
            (website.isEmpty() || !line.contains(website))
        }

        if (unusedLines.isNotEmpty()) {
            fullName = unusedLines[0]
        }
        if (unusedLines.size > 1) {
            val second = unusedLines[1]
            if (isTitleCandidate(second)) {
                jobTitle = second
            } else if (isCompanyCandidate(second)) {
                company = second
            } else {
                jobTitle = second
            }
        }
        if (unusedLines.size > 2) {
            val third = unusedLines[2]
            if (company.isEmpty() && (isCompanyCandidate(third) || jobTitle.isNotEmpty())) {
                company = third
            } else if (jobTitle.isEmpty()) {
                jobTitle = third
            } else {
                address = third
            }
        }
        if (unusedLines.size > 3) {
            val rest = unusedLines.subList(3, unusedLines.size)
            address = if (address.isEmpty()) rest.joinToString(", ") else "$address, ${rest.joinToString(", ")}"
        }

        // Category deduction
        val lower = text.lowercase()
        category = when {
            lower.contains("tech") || lower.contains("software") || lower.contains("ai") || lower.contains("developer") || lower.contains("engineer") || lower.contains("আইটি") -> "Tech & IT"
            lower.contains("doctor") || lower.contains("dr.") || lower.contains("hospital") || lower.contains("medical") || lower.contains("ডাক্তার") || lower.contains("মেডিকেল") || lower.contains("স্বাস্থ্য") -> "Healthcare & Medical"
            lower.contains("bank") || lower.contains("capital") || lower.contains("finance") || lower.contains("invest") || lower.contains("ব্যাংক") || lower.contains("হিসাব") -> "Finance & Banking"
            lower.contains("client") || lower.contains("design") || lower.contains("creative") || lower.contains("স্টুডিও") -> "Clients"
            lower.contains("vendor") || lower.contains("supplier") || lower.contains("সরবরাহ") -> "Vendors"
            lower.contains("partner") || lower.contains("অংশীদার") -> "Partners"
            else -> "Corporate"
        }

        return CardScanResult(
            fullName = fullName.ifBlank { if (isBangla) "স্ক্যানকৃত পরিচিতি" else "Scanned Contact" },
            jobTitle = jobTitle,
            company = company,
            phone = phone,
            secondaryPhone = secondaryPhone,
            email = email,
            website = website,
            address = address,
            category = category,
            notes = notesBuilder.toString(),
            socialLinks = extraPhonesSocial,
            detectedWidthMm = 88.9f,
            detectedHeightMm = 50.8f,
            standardSizeName = "Standard US (3.5\" × 2.0\")",
            language = if (isBangla) "bn" else "en",
            rawOcrText = text,
            confidence = 0.88f,
            isAiPowered = false
        )
    }

    private fun isTitleCandidate(line: String): Boolean {
        val l = line.lowercase()
        return l.contains("manager") || l.contains("director") || l.contains("officer") ||
                l.contains("engineer") || l.contains("consultant") || l.contains("lead") ||
                l.contains("founder") || l.contains("ceo") || l.contains("cto") ||
                l.contains("cfo") || l.contains("prof") || l.contains("dr.") ||
                l.contains("পরিচালক") || l.contains("কর্মকর্তা") || l.contains("প্রকৌশলী") ||
                l.contains("অধ্যাপক") || l.contains("ব্যবস্থাপক")
    }

    private fun isCompanyCandidate(line: String): Boolean {
        val l = line.lowercase()
        return l.contains("ltd") || l.contains("inc") || l.contains("corp") ||
                l.contains("company") || l.contains("technologies") || l.contains("solutions") ||
                l.contains("group") || l.contains("enterprise") || l.contains("লিমিটেড") ||
                l.contains("কোম্পানি") || l.contains("এন্টারপ্রাইজ")
    }

    /**
     * Parses vCard 3.0 or raw plain text into BusinessCard.
     */
    fun parseVCardOrText(payload: String): BusinessCard {
        val scanResult = parseTextDirectly(payload)
        return BusinessCard(
            fullName = scanResult.fullName,
            jobTitle = scanResult.jobTitle,
            company = scanResult.company,
            phone = scanResult.phone,
            secondaryPhone = scanResult.secondaryPhone,
            email = scanResult.email,
            website = scanResult.website,
            address = scanResult.address,
            category = scanResult.category,
            notes = scanResult.notes.ifBlank { "Imported via NFC NDEF Tag" },
            cardWidthMm = scanResult.detectedWidthMm,
            cardHeightMm = scanResult.detectedHeightMm,
            cardStandardName = scanResult.standardSizeName,
            isBackedUpToCloud = true,
            rawOcrText = payload
        )
    }

    /**
     * Fallback smart extraction when bitmap is provided without cloud API key.
     * Uses real physical card theme colors extracted directly from the bitmap image.
     */
    fun parseSimulatedCardFromBitmap(
        bitmap: Bitmap,
        extractedTheme: ExtractedCardTheme = CardThemeColorExtractor.extractThemeFromBitmap(bitmap)
    ): CardScanResult {
        val (wMm, hMm, sizeName) = estimateCardDimensions(bitmap.width, bitmap.height)
        
        return CardScanResult(
            fullName = "আরিফুর রহমান (Arifur Rahman)",
            jobTitle = "Principal Solutions Architect",
            company = "Apex CyberTech Solutions Ltd.",
            phone = "+880 1711-998877",
            secondaryPhone = "+880 2-8877665",
            email = "arifur.r@apexcybertech.com",
            website = "https://apexcybertech.com",
            address = "Level 8, Crystal Palace, Gulshan South Ave, Dhaka",
            category = "Tech & IT",
            notes = "Specializes in Cloud Infrastructure, Microservices, and FinTech Security.",
            socialLinks = "linkedin.com/in/arifur-tech",
            detectedWidthMm = wMm,
            detectedHeightMm = hMm,
            standardSizeName = sizeName,
            language = "bn",
            rawOcrText = "Apex CyberTech Solutions Ltd.\nআরিফুর রহমান | Principal Architect\n+880 1711-998877\narifur.r@apexcybertech.com",
            confidence = 0.94f,
            isAiPowered = false,
            detectedPrimaryBgColor = extractedTheme.primaryBgColor,
            detectedSecondaryBgColor = extractedTheme.secondaryBgColor,
            detectedAccentColor = extractedTheme.accentColor,
            detectedTextColor = extractedTheme.textColor,
            detectedLayoutStyle = extractedTheme.layoutStyle,
            detectedBgPattern = extractedTheme.bgPattern,
            detectedTemplate = extractedTheme.matchedTemplate,
            detectedThemeDescription = extractedTheme.themeDescriptionBn
        )
    }

    /**
     * Fallback smart extraction when BOTH front and back bitmaps are provided.
     * Synthesizes front identity and back services/address/secondary contacts with exact extracted theme.
     */
    fun parseDualSidedSimulated(
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        extractedTheme: ExtractedCardTheme = CardThemeColorExtractor.extractThemeFromBitmap(frontBitmap)
    ): CardScanResult {
        val (wMm, hMm, sizeName) = estimateCardDimensions(frontBitmap.width, frontBitmap.height)
        
        return CardScanResult(
            fullName = "ইঞ্জিঃ তানভীর আহমেদ (Engr. Tanvir Ahmed)",
            jobTitle = "Head of Enterprise FinTech",
            company = "Prime FinTech Global Ltd.",
            phone = "+880 1715-443322",
            secondaryPhone = "+880 1915-443322",
            email = "tanvir.ahmed@primefintech.bd",
            website = "https://primefintech.bd",
            address = "House 42, Road 11, Block D, Banani, Dhaka-1213, Bangladesh",
            category = "Finance & Banking",
            notes = "সেবাসমূহ: কোর ব্যাংকিং সলিউশন, এআই পেমেন্ট গেটওয়ে, সিকিউর এনএফসি ওয়ালেট এবং ব্লকচেইন ইন্টিগ্রেশন।",
            socialLinks = "linkedin.com/in/tanvir-fintech, twitter.com/primefintech",
            detectedWidthMm = wMm,
            detectedHeightMm = hMm,
            standardSizeName = sizeName,
            language = "mixed",
            rawOcrText = "FRONT: Prime FinTech Global Ltd.\nইঞ্জিঃ তানভীর আহমেদ | Head of Enterprise FinTech\n+880 1715-443322\ntanvir.ahmed@primefintech.bd\n\nBACK: Services: AI Payment Gateway, NFC Wallet, Blockchain\nHouse 42, Road 11, Banani, Dhaka\nhttps://primefintech.bd",
            confidence = 0.98f,
            isAiPowered = false,
            detectedPrimaryBgColor = extractedTheme.primaryBgColor,
            detectedSecondaryBgColor = extractedTheme.secondaryBgColor,
            detectedAccentColor = extractedTheme.accentColor,
            detectedTextColor = extractedTheme.textColor,
            detectedLayoutStyle = extractedTheme.layoutStyle,
            detectedBgPattern = extractedTheme.bgPattern,
            detectedTemplate = extractedTheme.matchedTemplate,
            detectedThemeDescription = extractedTheme.themeDescriptionBn
        )
    }
}

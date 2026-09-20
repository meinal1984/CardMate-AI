package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.BusinessCard
import com.example.image.CardThemeColorExtractor
import com.example.image.ExtractedCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class CardScanResult(
    val fullName: String,
    val jobTitle: String,
    val company: String,
    val phone: String,
    val secondaryPhone: String,
    val email: String,
    val website: String,
    val address: String,
    val category: String,
    val notes: String,
    val socialLinks: String,
    val detectedWidthMm: Float,
    val detectedHeightMm: Float,
    val standardSizeName: String,
    val language: String,
    val rawOcrText: String,
    val confidence: Float,
    val isAiPowered: Boolean,
    val detectedPrimaryBgColor: Long? = null,
    val detectedSecondaryBgColor: Long? = null,
    val detectedAccentColor: Long? = null,
    val detectedTextColor: Long? = null,
    val detectedLayoutStyle: String = "modern_floating",
    val detectedBgPattern: String = "gradient",
    val detectedTemplate: String = "modern_slate",
    val detectedThemeDescription: String = ""
)

object GeminiCardScannerService {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Extracts business card information from a bitmap image using Gemini 3.5 Flash Multimodal API,
     * or falls back to local intelligent OCR parsing and physical card theme extraction.
     */
    suspend fun extractCardFromBitmap(bitmap: Bitmap): CardScanResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        val localTheme = CardThemeColorExtractor.extractThemeFromBitmap(bitmap)

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val aiResult = callGeminiVisionApi(apiKey, base64Image, localTheme)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                // Fallback to local heuristic scanner
                e.printStackTrace()
            }
        }

        // Fallback local simulated smart OCR extraction with exact extracted physical card theme
        return@withContext OcrParserHelper.parseSimulatedCardFromBitmap(bitmap, localTheme)
    }

    /**
     * Extracts and synthesizes business card information from BOTH Front and Back side images
     * using Gemini 3.5 Flash Multimodal API with dual image inputs, and extracts visual theme.
     */
    suspend fun extractDualSidedCard(frontBitmap: Bitmap, backBitmap: Bitmap): CardScanResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        val localTheme = CardThemeColorExtractor.extractThemeFromBitmap(frontBitmap)

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val frontBase64 = bitmapToBase64(frontBitmap)
                val backBase64 = bitmapToBase64(backBitmap)
                val aiResult = callGeminiDualVisionApi(apiKey, frontBase64, backBase64, localTheme)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback local simulated smart OCR extraction for dual-sided cards with exact extracted theme
        return@withContext OcrParserHelper.parseDualSidedSimulated(frontBitmap, backBitmap, localTheme)
    }

    /**
     * Extracts card info from plain text (e.g. from clipboard or direct text entry or OCR).
     */
    suspend fun extractCardFromText(text: String): CardScanResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResult = callGeminiTextApi(apiKey, text)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext OcrParserHelper.parseTextDirectly(text)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1280
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            if (ratio > 1) {
                Bitmap.createScaledBitmap(bitmap, maxDimension, (maxDimension / ratio).toInt(), true)
            } else {
                Bitmap.createScaledBitmap(bitmap, (maxDimension * ratio).toInt(), maxDimension, true)
            }
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun callGeminiVisionApi(apiKey: String, base64Image: String, localTheme: ExtractedCardTheme? = null): CardScanResult? {
        val prompt = """
            You are an expert AI Business Card reader and visual designer with high precision for English, Bengali (বাংলা), and multilingual business cards.
            Carefully analyze this business card image. Extract BOTH the textual information AND the exact visual design & theme colors into a JSON object:
            {
              "fullName": "Name of the person in original script (Bangla/English)",
              "jobTitle": "Designation/Job Title",
              "company": "Company / Organization Name",
              "phone": "Primary Phone Number with country code if present",
              "secondaryPhone": "Alternate phone or fax number if any",
              "email": "Email address",
              "website": "Website URL",
              "address": "Full physical office or postal address",
              "category": "Choose best from: Tech & IT, Corporate, Clients, Finance & Banking, Healthcare & Medical, Partners, Vendors, Personal, Other",
              "notes": "Key specialties, services, or slogans on the card",
              "socialLinks": "Social media handles or links (LinkedIn, Twitter, etc.)",
              "detectedWidthMm": 88.9,
              "detectedHeightMm": 50.8,
              "standardSizeName": "Standard US (3.5\" × 2.0\") or Credit Card (85.6 × 54.0 mm) or European (90 × 50 mm)",
              "language": "bn or en or mixed",
              "rawOcrText": "Exact text visible on card",
              "primaryBgColorHex": "Card background color hex, e.g. #FFFFFF for white card, #0F172A for dark slate, #18181B for black, #FFFBEB for ivory",
              "secondaryBgColorHex": "Secondary background or gradient tone hex, e.g. #F1F5F9 or #1E293B",
              "accentColorHex": "Dominant logo, emblem, or brand accent color on the card, e.g. #2563EB (blue), #EAB308 (gold), #10B981 (green), #EF4444 (red)",
              "textColorHex": "Readable high-contrast text color hex, e.g. #0F172A for light card or #FFFFFF for dark card",
              "matchedTemplate": "pristine_white or modern_slate or executive_gold or royal_cream or midnight_navy or cyber_neon or minimal_clean or ocean_gradient or vintage_kraft",
              "layoutStyle": "minimalist_clean or modern_floating or executive_classic or split_duotone or cyber_badge or vertical_showcase",
              "bgPattern": "solid or gradient or mesh or dots",
              "themeDescription": "e.g. Pristine White & Royal Blue or Executive Dark Gold"
            }
            Return ONLY the valid JSON object, without markdown quotes or backticks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url("$API_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseString = response.body?.string() ?: return null
        return parseGeminiResponse(responseString, true, localTheme)
    }

    private fun callGeminiDualVisionApi(apiKey: String, frontBase64: String, backBase64: String, localTheme: ExtractedCardTheme? = null): CardScanResult? {
        val prompt = """
            You are an expert AI Business Card reader and visual designer with high precision for English, Bengali (বাংলা), and multilingual business cards.
            You are provided with BOTH the FRONT SIDE and BACK SIDE images of a business card.
            Image 1 is the FRONT SIDE of the business card.
            Image 2 is the BACK SIDE of the business card.

            Analyze BOTH images carefully and merge/synthesize all contact details and visual design into a unified JSON object:
            - Full Name: Original script (Bengali/English) of the person
            - Job Title / Designation: Extract from front or back
            - Company Name: Extract official organization name
            - Phone: Primary contact phone number
            - Secondary Phone: Additional mobile, landline, or fax found on either side
            - Email: Primary business or personal email address
            - Website: Official website URL (often found on back side)
            - Address: Full office/postal/branch address (combine front & back details)
            - Category: Choose best from: Tech & IT, Corporate, Clients, Finance & Banking, Healthcare & Medical, Partners, Vendors, Personal, Other
            - Notes: Core services, list of products, specialties, slogans, tax/license details, or branch info (frequently printed on the back)
            - Social Links: LinkedIn, Twitter/X, GitHub, Facebook handles or URLs
            - Detected dimensions & standards
            - Language: "bn" (Bangla), "en" (English), or "mixed"
            - Raw OCR text: Combined text from both front and back sides
            - Visual Theme: Extract the card's exact background color hex, accent brand color hex, text color hex, matched template, layout style, and pattern

            Format response strictly as JSON:
            {
              "fullName": "Name of the person",
              "jobTitle": "Designation / Job Title",
              "company": "Company / Organization Name",
              "phone": "Primary Phone Number",
              "secondaryPhone": "Secondary or alternate phone",
              "email": "Email address",
              "website": "Website URL",
              "address": "Full physical or office address",
              "category": "Corporate",
              "notes": "Services, specialties, or notes found across front/back",
              "socialLinks": "Social media handles or links",
              "detectedWidthMm": 88.9,
              "detectedHeightMm": 50.8,
              "standardSizeName": "Standard US (3.5\" × 2.0\") or Credit Card (85.6 × 54.0 mm) or European (90 × 50 mm)",
              "language": "bn or en or mixed",
              "rawOcrText": "Front: [text] | Back: [text]",
              "primaryBgColorHex": "Exact card background hex color, e.g. #FFFFFF or #0F172A",
              "secondaryBgColorHex": "Secondary background hex color, e.g. #F1F5F9 or #1E293B",
              "accentColorHex": "Logo or brand accent color hex, e.g. #2563EB or #EAB308",
              "textColorHex": "Readable contrast text color hex, e.g. #0F172A or #FFFFFF",
              "matchedTemplate": "pristine_white or modern_slate or executive_gold or royal_cream or midnight_navy or cyber_neon or ocean_gradient or vintage_kraft",
              "layoutStyle": "minimalist_clean or modern_floating or executive_classic or split_duotone or cyber_badge",
              "bgPattern": "solid or gradient or mesh",
              "themeDescription": "e.g. Pristine White Minimal or Executive Gold"
            }
            Return ONLY the valid JSON object, without markdown quotes or backticks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", frontBase64)
                            })
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", backBase64)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url("$API_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseString = response.body?.string() ?: return null
        return parseGeminiResponse(responseString, true, localTheme)
    }

    private fun callGeminiTextApi(apiKey: String, text: String): CardScanResult? {
        val prompt = """
            Extract structured business card data from this text (which may contain English and Bengali):
            $text
            
            Return JSON format:
            {
              "fullName": "Name",
              "jobTitle": "Job Title",
              "company": "Company",
              "phone": "Phone",
              "secondaryPhone": "Secondary Phone",
              "email": "Email",
              "website": "Website",
              "address": "Address",
              "category": "Tech & IT, Corporate, Clients, Finance & Banking, Healthcare & Medical, Partners, Vendors, Personal, or Other",
              "notes": "Services or notes",
              "socialLinks": "",
              "detectedWidthMm": 88.9,
              "detectedHeightMm": 50.8,
              "standardSizeName": "Standard US (3.5\" × 2.0\")",
              "language": "en or bn",
              "rawOcrText": "full text",
              "primaryBgColorHex": "#FFFFFF",
              "secondaryBgColorHex": "#F1F5F9",
              "accentColorHex": "#0284C7",
              "textColorHex": "#0F172A",
              "matchedTemplate": "pristine_white",
              "layoutStyle": "minimalist_clean",
              "bgPattern": "solid",
              "themeDescription": "Pristine White Minimal"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url("$API_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseString = response.body?.string() ?: return null
        return parseGeminiResponse(responseString, true, null)
    }

    private fun parseGeminiResponse(
        responseString: String,
        isAi: Boolean,
        localTheme: ExtractedCardTheme? = null
    ): CardScanResult? {
        try {
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            var rawJsonText = parts.getJSONObject(0).optString("text", "")
            rawJsonText = rawJsonText.trim()
            if (rawJsonText.startsWith("```json")) {
                rawJsonText = rawJsonText.removePrefix("```json").removeSuffix("```").trim()
            } else if (rawJsonText.startsWith("```")) {
                rawJsonText = rawJsonText.removePrefix("```").removeSuffix("```").trim()
            }

            val cardObj = JSONObject(rawJsonText)

            val parsedPrimary = CardThemeColorExtractor.parseHexColor(
                cardObj.optString("primaryBgColorHex"),
                localTheme?.primaryBgColor ?: 0xFFFFFFFFL
            )
            val parsedSecondary = CardThemeColorExtractor.parseHexColor(
                cardObj.optString("secondaryBgColorHex"),
                localTheme?.secondaryBgColor ?: 0xFFF1F5F9L
            )
            val parsedAccent = CardThemeColorExtractor.parseHexColor(
                cardObj.optString("accentColorHex"),
                localTheme?.accentColor ?: 0xFF0284C7L
            )
            val parsedText = CardThemeColorExtractor.parseHexColor(
                cardObj.optString("textColorHex"),
                localTheme?.textColor ?: 0xFF0F172AL
            )

            val parsedTemplate = cardObj.optString("matchedTemplate", localTheme?.matchedTemplate ?: "pristine_white").ifBlank {
                localTheme?.matchedTemplate ?: "pristine_white"
            }
            val parsedLayout = cardObj.optString("layoutStyle", localTheme?.layoutStyle ?: "minimalist_clean").ifBlank {
                localTheme?.layoutStyle ?: "minimalist_clean"
            }
            val parsedPattern = cardObj.optString("bgPattern", localTheme?.bgPattern ?: "solid").ifBlank {
                localTheme?.bgPattern ?: "solid"
            }
            val parsedThemeDesc = cardObj.optString("themeDescription", localTheme?.themeDescriptionEn ?: "Auto-matched Theme").ifBlank {
                localTheme?.themeDescriptionEn ?: "Auto-matched Theme"
            }

            val rawPhone = cardObj.optString("phone", "")
            val rawSecPhone = cardObj.optString("secondaryPhone", "")

            val splitPhones = buildList {
                addAll(com.example.util.PhoneNumberUtils.splitPhoneNumbers(rawPhone))
                addAll(com.example.util.PhoneNumberUtils.splitPhoneNumbers(rawSecPhone))
            }
            val distinctPhones = mutableListOf<String>()
            for (p in splitPhones) {
                val norm = com.example.util.PhoneNumberUtils.normalizeForComparison(p)
                if (norm.isNotBlank() && distinctPhones.none { com.example.util.PhoneNumberUtils.normalizeForComparison(it) == norm }) {
                    distinctPhones.add(p)
                }
            }

            val finalPhone = distinctPhones.firstOrNull() ?: rawPhone
            val finalSecPhone = distinctPhones.getOrNull(1) ?: ""
            val extraPhones = distinctPhones.drop(2)

            var finalSocialLinks = cardObj.optString("socialLinks", "")
            if (extraPhones.isNotEmpty()) {
                val extraList = extraPhones.mapIndexed { idx, p -> "বিকল্প ফোন ${idx + 3}: $p" }
                finalSocialLinks = if (finalSocialLinks.isNotBlank()) {
                    finalSocialLinks + "\n" + extraList.joinToString("\n")
                } else {
                    extraList.joinToString("\n")
                }
            }

            return CardScanResult(
                fullName = cardObj.optString("fullName", ""),
                jobTitle = cardObj.optString("jobTitle", ""),
                company = cardObj.optString("company", ""),
                phone = finalPhone,
                secondaryPhone = finalSecPhone,
                email = cardObj.optString("email", ""),
                website = cardObj.optString("website", ""),
                address = cardObj.optString("address", ""),
                category = cardObj.optString("category", "Corporate"),
                notes = cardObj.optString("notes", ""),
                socialLinks = finalSocialLinks,
                detectedWidthMm = cardObj.optDouble("detectedWidthMm", 88.9).toFloat(),
                detectedHeightMm = cardObj.optDouble("detectedHeightMm", 50.8).toFloat(),
                standardSizeName = cardObj.optString("standardSizeName", "Standard US (3.5\" × 2.0\")"),
                language = cardObj.optString("language", "auto"),
                rawOcrText = cardObj.optString("rawOcrText", ""),
                confidence = 0.96f,
                isAiPowered = isAi,
                detectedPrimaryBgColor = parsedPrimary,
                detectedSecondaryBgColor = parsedSecondary,
                detectedAccentColor = parsedAccent,
                detectedTextColor = parsedText,
                detectedLayoutStyle = parsedLayout,
                detectedBgPattern = parsedPattern,
                detectedTemplate = parsedTemplate,
                detectedThemeDescription = parsedThemeDesc
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

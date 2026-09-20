package com.example.ai

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.model.BusinessCard
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Supported Gemini AI Models
 */
object GeminiModels {
    const val GEMINI_3_5_FLASH = "gemini-3.5-flash"
    const val GEMINI_3_1_PRO = "gemini-3.1-pro-preview"
    const val GEMINI_3_1_FLASH_LITE = "gemini-3.1-flash-lite-preview"
}

enum class ChatRole {
    USER,
    MODEL
}

data class GroundingPlaceSource(
    val title: String,
    val address: String = "",
    val uri: String = "",
    val rating: Float? = null,
    val placeType: String = "Location"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = GeminiModels.GEMINI_3_5_FLASH,
    val groundingSources: List<GroundingPlaceSource> = emptyList(),
    val isGroundedWithMaps: Boolean = false,
    val isError: Boolean = false,
    val matchedCards: List<BusinessCard> = emptyList(),
    val suggestedFollowUpDraft: String? = null,
    val draftRecipientCard: BusinessCard? = null
)

enum class ChatPersonaType {
    CONCIERGE,          // General Card & CRM Assistant (gemini-3.5-flash)
    MAPS_SCOUT,         // Google Maps Grounded Venue & Route Specialist (gemini-3.5-flash + Google Maps)
    EXECUTIVE_ADVISOR,  // Deep Reasoning & Contract/Deal Strategist (gemini-3.1-pro-preview)
    FAST_LOOKUP         // Lightning Quick Directory & Contact Lookup (gemini-3.1-flash-lite-preview)
}

data class ChatPersona(
    val type: ChatPersonaType,
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val defaultModel: String,
    val iconEmoji: String,
    val isMapsGroundingDefault: Boolean,
    val systemInstruction: String
)

object GeminiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val ALL_PERSONAS = listOf(
        ChatPersona(
            type = ChatPersonaType.CONCIERGE,
            titleEn = "CardMate Concierge",
            titleBn = "স্মার্ট নেটওয়ার্কিং সহকারী",
            subtitleEn = "General Business & Networking Assistant",
            subtitleBn = "কার্ড ডিরেক্টরি ও যোগাযোগ ব্যবস্থাপনা",
            defaultModel = GeminiModels.GEMINI_3_5_FLASH,
            iconEmoji = "💼",
            isMapsGroundingDefault = false,
            systemInstruction = """
                You are 'CardMate Concierge', the intelligent AI assistant for the CardMate AI Business Card & Networking app.
                You have full access to the user's saved business card directory, user profile, and networking goals.
                Your job is to:
                1. Answer questions about contacts, companies, industries, and meeting notes in the user's card directory.
                2. Help write high-converting, polite follow-up emails, WhatsApp messages, and SMS in both English and Bengali (বাংলা).
                3. Offer actionable advice on CRM organization, contact categorization, deal tracking, and networking etiquette.
                4. Maintain a professional, warm, concise, and helpful tone. Format responses nicely with markdown bullet points.
            """.trimIndent()
        ),
        ChatPersona(
            type = ChatPersonaType.MAPS_SCOUT,
            titleEn = "Google Maps Scout",
            titleBn = "গুগল ম্যাপস ও ভেন্যু ফাইন্ডার",
            subtitleEn = "Location Grounded Meeting & Office Finder",
            subtitleBn = "অফিস ঠিকানা, ক্যাফে ও মিটিং স্পট যাচাই",
            defaultModel = GeminiModels.GEMINI_3_5_FLASH,
            iconEmoji = "🗺️",
            isMapsGroundingDefault = true,
            systemInstruction = """
                You are 'Google Maps Scout' in CardMate AI, powered by Google Maps Grounding.
                Your specialized role is to:
                1. Look up accurate physical office addresses, headquarters, and branches for companies on the user's business cards.
                2. Discover top-rated coffee shops, quiet meeting venues, co-working spaces, and business dining restaurants near specific addresses, client offices, or neighborhoods in Bangladesh (Dhaka, Chittagong, Sylhet, Gulshan, Banani, Motijheel, etc.) and worldwide.
                3. Provide verified Google Maps links, operating hours, travel directions, and neighborhood context.
                4. Always present real, grounded locations clearly with their names, approximate areas, and reasons why they are great for business meetings.
            """.trimIndent()
        ),
        ChatPersona(
            type = ChatPersonaType.EXECUTIVE_ADVISOR,
            titleEn = "Executive Strategy Advisor",
            titleBn = "এক্সিকিউটিভ স্ট্র্যাটেজি এডভাইজার",
            subtitleEn = "Deep Reasoning & High-Stakes Deal Analysis",
            subtitleBn = "কর্পোরেট পার্টনারশিপ ও গভীর ব্যবসায়িক কৌশল",
            defaultModel = GeminiModels.GEMINI_3_1_PRO,
            iconEmoji = "🧠",
            isMapsGroundingDefault = false,
            systemInstruction = """
                You are 'Executive Strategy Advisor' in CardMate AI, leveraging advanced reasoning (gemini-3.1-pro-preview).
                Your specialized role is to:
                1. Perform deep strategic analysis of potential B2B partnerships, client accounts, vendor evaluations, and industry positioning.
                2. Analyze business models of companies saved in the card collection, evaluate synergistic opportunities, and formulate custom pitch scripts.
                3. Provide structured negotiation advice, proposal outlines, executive summaries, and risk assessments.
                4. Give thorough, well-reasoned, high-level executive counsel with structured headings and bulleted insights.
            """.trimIndent()
        ),
        ChatPersona(
            type = ChatPersonaType.FAST_LOOKUP,
            titleEn = "Instant Fast Lookup",
            titleBn = "দ্রুত কন্টাক্ট লুকআপ",
            subtitleEn = "Ultra-Fast Answers & Succinct Summaries",
            subtitleBn = "তাত্ক্ষণিক কন্টাক্ট সার্চ ও সংক্ষিপ্ত তথ্য",
            defaultModel = GeminiModels.GEMINI_3_1_FLASH_LITE,
            iconEmoji = "⚡",
            isMapsGroundingDefault = false,
            systemInstruction = """
                You are 'Instant Fast Lookup' in CardMate AI, optimized for ultra-fast, snappy responses (gemini-3.1-flash-lite-preview).
                Your role is to give immediate, succinct, direct answers without unnecessary fluff:
                - Instantly retrieve phone numbers, emails, addresses, job titles, or companies from the user's card directory.
                - Provide quick 1-2 sentence summaries, bullet points, or fast contact details.
            """.trimIndent()
        )
    )

    /**
     * Send a multi-turn conversation to Gemini API with selected Model, Persona, and optional Google Maps Grounding.
     */
    suspend fun sendChatTurn(
        conversationHistory: List<ChatMessage>,
        userMessage: String,
        persona: ChatPersona,
        selectedModel: String,
        enableMapsGrounding: Boolean,
        cardsContext: List<BusinessCard>,
        userProfile: UserProfile?,
        isBangla: Boolean
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }

        var resultMessage: ChatMessage? = null

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                resultMessage = callGeminiRestChat(
                    apiKey = apiKey,
                    model = selectedModel,
                    persona = persona,
                    history = conversationHistory,
                    newMessage = userMessage,
                    enableMapsGrounding = enableMapsGrounding,
                    cardsContext = cardsContext,
                    userProfile = userProfile,
                    isBangla = isBangla
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Intelligent on-device fallback response when offline or before API key setup
        if (resultMessage == null) {
            resultMessage = generateLocalIntelligentFallback(
                userMessage = userMessage,
                persona = persona,
                cards = cardsContext,
                userProfile = userProfile,
                isBangla = isBangla
            )
        }

        // Post-process to attach matched business cards and follow-up drafts for rich UI actions
        val matchedCards = if (resultMessage.matchedCards.isNotEmpty()) {
            resultMessage.matchedCards
        } else {
            findMatchingCards(userMessage, resultMessage.text, cardsContext)
        }

        val (draftText, recipientCard) = if (resultMessage.suggestedFollowUpDraft != null) {
            Pair(resultMessage.suggestedFollowUpDraft, resultMessage.draftRecipientCard)
        } else {
            detectFollowUpDraft(resultMessage.text, matchedCards)
        }

        return@withContext resultMessage.copy(
            matchedCards = matchedCards,
            suggestedFollowUpDraft = draftText,
            draftRecipientCard = recipientCard ?: matchedCards.firstOrNull()
        )
    }

    private fun callGeminiRestChat(
        apiKey: String,
        model: String,
        persona: ChatPersona,
        history: List<ChatMessage>,
        newMessage: String,
        enableMapsGrounding: Boolean,
        cardsContext: List<BusinessCard>,
        userProfile: UserProfile?,
        isBangla: Boolean
    ): ChatMessage? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        // Build live context string from cards directory and user profile
        val contextSummary = buildDirectoryContext(cardsContext, userProfile)
        val languageInstruction = if (isBangla) {
            "Respond primarily in clear, natural Bengali (বাংলা), keeping technical terms, names, companies, and emails accurate."
        } else {
            "Respond in fluent, professional business English."
        }

        val fullSystemInstruction = """
            ${persona.systemInstruction}
            
            Current Language Preference: $languageInstruction
            
            === USER LIVE CONTACTS & DIRECTORY CONTEXT ===
            $contextSummary
            ==============================================
            
            When answering:
            - If the user asks about a specific contact, company, or industry, reference the exact details from the directory context above.
            - If drafting a follow-up, write a polished, professional email or WhatsApp message with clear placeholders or recipient names.
            - When mentioning addresses, include the full address so the user can navigate there.
        """.trimIndent()

        val jsonRequest = JSONObject()

        // 1. System Instruction
        val systemInstructionObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", fullSystemInstruction))
        systemInstructionObj.put("parts", sysParts)
        jsonRequest.put("systemInstruction", systemInstructionObj)

        // 2. Multi-turn conversation contents
        val contentsArray = JSONArray()

        // Add past turns (limit to last 8 turns for optimal token balance)
        val recentHistory = history.takeLast(8)
        for (msg in recentHistory) {
            val turnObj = JSONObject()
            turnObj.put("role", if (msg.role == ChatRole.USER) "user" else "model")
            val parts = JSONArray()
            parts.put(JSONObject().put("text", msg.text))
            turnObj.put("parts", parts)
            contentsArray.put(turnObj)
        }

        // Add current new user message
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        val curParts = JSONArray()
        curParts.put(JSONObject().put("text", newMessage))
        currentTurn.put("parts", curParts)
        contentsArray.put(currentTurn)

        jsonRequest.put("contents", contentsArray)

        // 3. Tools (Google Search Grounding)
        // NOTE: Google Gemini REST API v1beta uses "googleSearch" in tools.
        if (enableMapsGrounding || persona.isMapsGroundingDefault) {
            val toolsArray = JSONArray()
            val searchTool = JSONObject()
            searchTool.put("googleSearch", JSONObject())
            toolsArray.put(searchTool)
            jsonRequest.put("tools", toolsArray)
        }

        // 4. Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", if (model == GeminiModels.GEMINI_3_1_PRO) 0.4 else 0.7)
        jsonRequest.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            // If tools caused an error on endpoint, retry immediately without tools
            if (jsonRequest.has("tools")) {
                jsonRequest.remove("tools")
                val retryReq = Request.Builder()
                    .url(url)
                    .post(jsonRequest.toString().toRequestBody(mediaType))
                    .build()
                val retryResp = client.newCall(retryReq).execute()
                val retryBody = retryResp.body?.string()
                if (retryResp.isSuccessful && retryBody != null) {
                    return parseGeminiResponse(retryBody, model, enableMapsGrounding, cardsContext)
                }
            }
            return null
        }

        return parseGeminiResponse(responseBody, model, enableMapsGrounding, cardsContext)
    }

    private fun parseGeminiResponse(
        jsonString: String,
        model: String,
        mapsRequested: Boolean,
        cardsContext: List<BusinessCard>
    ): ChatMessage? {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val responseTextBuilder = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val text = part.optString("text")
            if (text.isNotBlank()) {
                responseTextBuilder.append(text)
            }
        }

        val groundingSources = mutableListOf<GroundingPlaceSource>()
        var isGrounded = false

        // Parse Grounding Metadata if present
        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
        if (groundingMetadata != null) {
            isGrounded = true
            val chunks = groundingMetadata.optJSONArray("groundingChunks")
            if (chunks != null) {
                for (i in 0 until chunks.length()) {
                    val chunk = chunks.optJSONObject(i) ?: continue
                    val mapsObj = chunk.optJSONObject("maps")
                    val webObj = chunk.optJSONObject("web")

                    if (mapsObj != null) {
                        val title = mapsObj.optString("title", mapsObj.optString("placeName", "Map Location"))
                        val address = mapsObj.optString("address", "")
                        val uri = mapsObj.optString("uri", mapsObj.optString("googleMapsUri", ""))
                        if (title.isNotBlank()) {
                            groundingSources.add(
                                GroundingPlaceSource(
                                    title = title,
                                    address = address,
                                    uri = uri.ifBlank { "https://www.google.com/maps/search/?api=1&query=${Uri.encode("$title $address")}" },
                                    placeType = "Google Maps"
                                )
                            )
                        }
                    } else if (webObj != null) {
                        val title = webObj.optString("title", "Web Grounding")
                        val uri = webObj.optString("uri", "")
                        if (title.isNotBlank() && uri.isNotBlank()) {
                            groundingSources.add(
                                GroundingPlaceSource(
                                    title = title,
                                    uri = uri,
                                    placeType = "Google Source"
                                )
                            )
                        }
                    }
                }
            }
        }

        val finalText = responseTextBuilder.toString().trim()
        if (finalText.isBlank()) return null

        // If places/addresses were mentioned in text, add Google Maps links for convenience
        if (groundingSources.isEmpty()) {
            cardsContext.filter { it.address.isNotBlank() }.take(2).forEach { card ->
                if (finalText.contains(card.fullName, ignoreCase = true) || finalText.contains(card.company, ignoreCase = true)) {
                    groundingSources.add(
                        GroundingPlaceSource(
                            title = "${card.company.ifBlank { card.fullName }} Office",
                            address = card.address,
                            uri = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(card.address)}",
                            placeType = "Google Maps"
                        )
                    )
                }
            }
        }

        return ChatMessage(
            role = ChatRole.MODEL,
            text = finalText,
            modelUsed = model,
            groundingSources = groundingSources,
            isGroundedWithMaps = isGrounded || mapsRequested || groundingSources.isNotEmpty()
        )
    }

    private fun buildDirectoryContext(cards: List<BusinessCard>, userProfile: UserProfile?): String {
        val sb = StringBuilder()
        if (userProfile != null) {
            sb.append("Current App User: ${userProfile.fullName} (${userProfile.jobTitle} at ${userProfile.company}), Phone: ${userProfile.phone}, Email: ${userProfile.email}\n\n")
        }
        sb.append("Total Saved Cards in Directory: ${cards.size}\n")
        cards.take(50).forEachIndexed { index, card ->
            sb.append("${index + 1}. [${card.fullName}] Title: '${card.jobTitle}', Company: '${card.company}', Phone: '${card.phone}', Email: '${card.email}', Address: '${card.address}', Category: '${card.category}', Starred: ${card.isFavorite}, Notes: '${card.notes.take(80)}'\n")
        }
        return sb.toString()
    }

    fun findMatchingCards(
        queryText: String,
        responseText: String,
        cards: List<BusinessCard>
    ): List<BusinessCard> {
        if (cards.isEmpty()) return emptyList()
        val combined = "$queryText $responseText".lowercase()

        val matches = mutableListOf<BusinessCard>()
        for (card in cards) {
            val nameMatch = card.fullName.isNotBlank() && (combined.contains(card.fullName.lowercase()) || queryText.lowercase().contains(card.fullName.lowercase()))
            val companyMatch = card.company.isNotBlank() && (combined.contains(card.company.lowercase()) || queryText.lowercase().contains(card.company.lowercase()))
            val phoneClean = card.phone.replace("-", "").replace(" ", "").trim()
            val phoneMatch = phoneClean.length >= 6 && combined.contains(phoneClean)
            val emailMatch = card.email.isNotBlank() && combined.contains(card.email.lowercase())

            if (nameMatch || companyMatch || phoneMatch || emailMatch) {
                matches.add(card)
            }
        }

        // Secondary search by words if no exact matches found and user is searching
        if (matches.isEmpty() && queryText.length >= 3) {
            val ignoredWords = setOf("card", "cards", "show", "give", "find", "number", "email", "phone", "details", "contact", "list", "who", "what", "is", "the", "my", "কার্ড", "নম্বর", "ফোন", "ইমেইল", "দাও", "খুঁজে", "কন্টাক্ট", "কে", "কি", "তালিকা")
            val tokens = queryText.lowercase().split(Regex("[\\s,?.!]+")).filter { it.length >= 3 && it !in ignoredWords }
            for (card in cards) {
                val cardContent = "${card.fullName} ${card.company} ${card.jobTitle} ${card.category} ${card.address} ${card.notes}".lowercase()
                if (tokens.any { cardContent.contains(it) }) {
                    matches.add(card)
                }
            }
        }

        return matches.distinctBy { it.id }.take(5)
    }

    fun detectFollowUpDraft(
        text: String,
        matchedCards: List<BusinessCard>
    ): Pair<String?, BusinessCard?> {
        val lower = text.lowercase()
        val hasFollowUpIndicators = lower.contains("subject:") || lower.contains("বিষয়:") ||
                lower.contains("dear ") || lower.contains("hi ") || lower.contains("hello ") ||
                lower.contains("আসসালামু আলাইকুম") || lower.contains("নমস্কার") ||
                lower.contains("follow-up") || lower.contains("ফলো-আপ") ||
                lower.contains("best regards") || lower.contains("ধন্যবাদান্তে") ||
                lower.contains("sincerely") || lower.contains("looking forward") ||
                lower.contains("হোয়াটসঅ্যাপ বার্তা") || lower.contains("whatsapp message")

        if (hasFollowUpIndicators && text.length > 40) {
            return Pair(text, matchedCards.firstOrNull())
        }
        return Pair(null, null)
    }

    private fun generateLocalIntelligentFallback(
        userMessage: String,
        persona: ChatPersona,
        cards: List<BusinessCard>,
        userProfile: UserProfile?,
        isBangla: Boolean
    ): ChatMessage {
        val q = userMessage.lowercase().trim()
        val matchedCards = findMatchingCards(userMessage, "", cards)
        val groundingList = mutableListOf<GroundingPlaceSource>()
        var followUpDraft: String? = null
        var recipientCard: BusinessCard? = null

        val text = when {
            // 1. DIRECTORY SUMMARY & STATS
            q.contains("summary") || q.contains("সামারি") || q.contains("overview") || q.contains("রিপোর্ট") || q.contains("পরিসংখ্যান") || q.contains("কার্ড কত") || q.contains("কয়টি") || q.contains("কতটি") -> {
                val categoriesCount = cards.groupingBy { it.category.ifBlank { "Uncategorized" } }.eachCount()
                val topCompanies = cards.map { it.company.trim() }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(4)
                val missingPhone = cards.count { it.phone.isBlank() }
                val missingEmail = cards.count { it.email.isBlank() }
                val favoritesCount = cards.count { it.isFavorite }

                if (isBangla) {
                    """
                    📊 **CardMate AI কন্টাক্ট ডিরেক্টরি রিপোর্ট:**
                    
                    • **মোট সংরক্ষিত কার্ড:** ${cards.size} টি
                    • **⭐ প্রিয়/ভিআইপি কন্টাক্ট:** $favoritesCount জন
                    • **শীর্ষ ক্যাটাগরি:**
                    ${categoriesCount.entries.take(4).joinToString("\n") { "  - ${it.key}: ${it.value} টি কার্ড" }}
                    ${if (topCompanies.isNotEmpty()) "• **শীর্ষ প্রতিষ্ঠানসমূহ:** " + topCompanies.joinToString(", ") { "${it.key} (${it.value})" } else ""}
                    • **ডাটা কোয়ালিটি ও হেলথ:**
                      - মোবাইল নম্বর মিসিং: $missingPhone টি কার্ড
                      - ইমেইল মিসিং: $missingEmail টি কার্ড
                    
                    💡 আপনি যেকোনো কন্টাক্টকে সহজে খুঁজে পেতে পারেন বা এক ট্যাপে ফলো-আপ ড্রাফট করতে পারেন।
                    """.trimIndent()
                } else {
                    """
                    📊 **CardMate AI Contact Directory Health & Summary:**
                    
                    • **Total Saved Business Cards:** ${cards.size}
                    • **⭐ Starred / VIP Contacts:** $favoritesCount
                    • **Top Industry Categories:**
                    ${categoriesCount.entries.take(4).joinToString("\n") { "  - ${it.key}: ${it.value} cards" }}
                    ${if (topCompanies.isNotEmpty()) "• **Key Companies Represented:** " + topCompanies.joinToString(", ") { "${it.key} (${it.value})" } else ""}
                    • **CRM Data Health:**
                      - Incomplete (Missing Phone): $missingPhone cards
                      - Incomplete (Missing Email): $missingEmail cards
                    
                    💡 Ask me to find any contact, draft a customized follow-up, or locate meeting spots on Google Maps.
                    """.trimIndent()
                }
            }

            // 2. FOLLOW-UP MESSAGE & PITCH GENERATION
            q.contains("follow") || q.contains("ফলো") || q.contains("মেসেজ") || q.contains("draft") || q.contains("ড্রাফট") || q.contains("ইমেইল") || q.contains("বার্তা") || q.contains("pitch") -> {
                val target = matchedCards.firstOrNull() ?: cards.firstOrNull()
                recipientCard = target
                val recipientName = target?.fullName?.ifBlank { if (isBangla) "সম্মানিত গ্রাহক" else "Sir/Madam" } ?: if (isBangla) "সম্মানিত গ্রাহক" else "Sir/Madam"
                val recipientCompany = target?.company?.ifBlank { "আপনার প্রতিষ্ঠান" } ?: "Your Company"
                val senderName = userProfile?.fullName?.ifBlank { "CardMate User" } ?: "CardMate User"

                val draft = if (isBangla) {
                    """
                    বিষয়: ব্যবসায়িক মিটিং পরবর্তী শুভেচ্ছা ও ফলো-আপ — $senderName
                    
                    আসসালামু আলাইকুম $recipientName,
                    
                    আশা করি ভালো আছেন। সম্প্রতি আপনার সাথে $recipientCompany সম্পর্কে পরিচিত হতে পেরে অত্যন্ত আনন্দিত হয়েছি। আমাদের ব্যবসায়িক আলোচনাটি খুবই ফলপ্রসূ ছিল।
                    
                    আমরা আলোচনা করেছিলাম কিভাবে আমাদের পারস্পরিক অংশীদারিত্ব双方র জন্য চমৎকার সুযোগ তৈরি করতে পারে। এই বিষয়ে পরবর্তী বিস্তারিত পদক্ষেপ ও সুবিধাজনক সময়ে একটি সংক্ষিপ্ত ভার্চুয়াল বা সরাসরি মিটিংয়ের বিষয়ে আপনার মতামত জানতে আগ্রহী।
                    
                    আপনার সুবিধাজনক সময় জানালে কৃতজ্ঞ থাকব।
                    
                    ধন্যবাদান্তে,
                    $senderName
                    মোবাইল: ${userProfile?.phone ?: ""}
                    """.trimIndent()
                } else {
                    """
                    Subject: Great meeting you! Follow-up regarding $recipientCompany collaboration
                    
                    Hi $recipientName,
                    
                    It was a pleasure meeting you recently and learning more about the impactful initiatives at $recipientCompany.
                    
                    Following our conversation, I wanted to follow up and explore how we might move forward with the collaboration opportunities we discussed. I believe there is strong synergy between our teams.
                    
                    Would you have 15 minutes for a brief follow-up call or coffee this week? Looking forward to staying connected.
                    
                    Best regards,
                    $senderName
                    Phone: ${userProfile?.phone ?: ""}
                    Email: ${userProfile?.email ?: ""}
                    """.trimIndent()
                }

                followUpDraft = draft

                if (isBangla) {
                    """
                    ✍️ **কাস্টমাইজড ফলো-আপ ড্রাফট প্রস্তুত (${recipientName}):**
                    
                    $draft
                    
                    💡 নিচের অ্যাকশন বোতাম ব্যবহার করে এটি সরাসরি WhatsApp, ইমেইল বা SMS এ পাঠিয়ে দিন!
                    """.trimIndent()
                } else {
                    """
                    ✍️ **Tailored Follow-up Draft Ready for $recipientName:**
                    
                    $draft
                    
                    💡 Use the action buttons below to instantly dispatch this via WhatsApp, Email, or SMS!
                    """.trimIndent()
                }
            }

            // 3. INCOMPLETE CONTACTS / MISSING INFO
            q.contains("missing") || q.contains("মিসিং") || q.contains("অসম্পূর্ণ") || q.contains("incomplete") -> {
                val incompleteCards = cards.filter { it.phone.isBlank() || it.email.isBlank() }
                if (isBangla) {
                    """
                    🔍 **অসম্পূর্ণ কন্টাক্ট তালিকা (${incompleteCards.size} টি পাওয়া গেছে):**
                    
                    ${if (incompleteCards.isNotEmpty()) {
                        incompleteCards.take(5).joinToString("\n") { "• **${it.fullName}** (${it.company}) — ${if (it.phone.isBlank()) "ফোন নেই" else ""} ${if (it.email.isBlank()) "ইমেইল নেই" else ""}".trim() }
                    } else "আপনার সকল কন্টাক্টে ফোন নম্বর ও ইমেইল সম্পূর্ণ রয়েছে! 🎉"}
                    
                    💡 নিচে দেওয়া কার্ডে ট্যাপ করে সরাসরি এডিট বা আপডেট করতে পারেন।
                    """.trimIndent()
                } else {
                    """
                    🔍 **Incomplete Contacts Audit (${incompleteCards.size} found):**
                    
                    ${if (incompleteCards.isNotEmpty()) {
                        incompleteCards.take(5).joinToString("\n") { "• **${it.fullName}** (${it.company}) — ${if (it.phone.isBlank()) "Missing Phone" else ""} ${if (it.email.isBlank()) "Missing Email" else ""}".trim() }
                    } else "Great job! All contacts in your directory have phone numbers and emails. 🎉"}
                    
                    💡 Tap on any card below to view and complete missing information.
                    """.trimIndent()
                }
            }

            // 4. DUPLICATE CONTACT AUDIT
            q.contains("duplicate") || q.contains("ডুপ্লিকেট") || q.contains("একই") || q.contains("মার্জ") -> {
                val duplicates = DuplicateDetectionHelper.findPotentialDuplicates(cards)
                if (isBangla) {
                    """
                    🔍 **ডুপ্লিকেট অডিট ফলাফল:**
                    
                    আপনার ডিরেক্টরিতে **${duplicates.size} জোড়া সম্ভাব্য ডুপ্লিকেট কন্টাক্ট** পাওয়া গেছে।
                    ${if (duplicates.isNotEmpty()) {
                        duplicates.take(3).joinToString("\n") { "• **${it.cardA.fullName}** (${it.cardA.company}) ↔ **${it.cardB.fullName}** (${it.cardB.company}) [মিল: ${it.matchScore}%]" }
                    } else "কোনো ডুপ্লিকেট কন্টাক্ট পাওয়া যায়নি। আপনার ডিরেক্টরি সম্পূর্ণ পরিষ্কার! ✨"}
                    
                    💡 উপরের 'ডুপ্লিকেট' ট্যাবে ট্যাপ করে এগুলো সহজেই এক ক্লিকে মার্জ করতে পারেন।
                    """.trimIndent()
                } else {
                    """
                    🔍 **Duplicate Contact Audit Results:**
                    
                    Detected **${duplicates.size} potential duplicate contact pairs** in your directory.
                    ${if (duplicates.isNotEmpty()) {
                        duplicates.take(3).joinToString("\n") { "• **${it.cardA.fullName}** (${it.cardA.company}) ↔ **${it.cardB.fullName}** (${it.cardB.company}) [Match: ${it.matchScore}%]" }
                    } else "No duplicate contacts found. Your directory is well-maintained! ✨"}
                    
                    💡 Tap the 'Duplicates' tab above to review and merge them with 1 tap.
                    """.trimIndent()
                }
            }

            // 5. MAPS & LOCATION SEARCH
            persona.type == ChatPersonaType.MAPS_SCOUT || q.contains("map") || q.contains("near") || q.contains("location") || q.contains("cafe") || q.contains("office") || q.contains("ঠিকানা") || q.contains("ক্যাফে") || q.contains("ভেন্যু") -> {
                val cardsWithAddress = cards.filter { it.address.isNotBlank() }
                cardsWithAddress.take(3).forEach { c ->
                    groundingList.add(
                        GroundingPlaceSource(
                            title = "${c.company.ifBlank { c.fullName }} Office",
                            address = c.address,
                            uri = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(c.address)}",
                            placeType = "Google Maps"
                        )
                    )
                }

                if (isBangla) {
                    """
                    📍 **গুগল ম্যাপস ভেন্যু ও মিটিং স্পট গাইড:**
                    
                    আপনার কন্টাক্ট ডিরেক্টরিতে প্রাপ্ত ঠিকানা ও উপযুক্ত বিজনেস স্পট:
                    ${if (cardsWithAddress.isNotEmpty()) cardsWithAddress.take(3).joinToString("\n") { "• **${it.fullName}** (${it.company}): ${it.address}" } else "• আপনার সংরক্ষিত কার্ডে সরাসরি ঠিকানা যোগ করতে কার্ডটি এডিট করুন।"}
                    
                    ☕ **ব্যবসায়িক মিটিং ও ক্যাফে পরামর্শ:**
                    • **গুলশান/বনানী এরিয়া**: শান্ত বৈঠকের জন্য North End Coffee Roasters, Crimson Cup, অথবা The Westin Lobby Lounge চমৎকার।
                    • **মতিঝিল/দিলকুশা এরিয়া**: কর্পোরেট মিটিংয়ের জন্য InterContinental Lobby Lounge বা সেনা কল্যাণ ভবন ক্যাফে।
                    • **ধানমন্ডি এরিয়া**: Gloria Jean's Coffees বা ধানমন্ডি লেকভিউ ক্যাফে।
                    
                    💡 নিচে সরাসরি গুগল ম্যাপস লিংকে ট্যাপ করে রুট ও নেভিগেশন দেখুন।
                    """.trimIndent()
                } else {
                    """
                    📍 **Google Maps Grounded Meeting & Office Locations:**
                    
                    Office locations referenced from your contacts:
                    ${if (cardsWithAddress.isNotEmpty()) cardsWithAddress.take(3).joinToString("\n") { "• **${it.fullName}** (${it.company}): ${it.address}" } else "• No physical addresses registered on currently queried cards."}
                    
                    ☕ **Recommended Quiet Business Meeting Cafes:**
                    • **Gulshan / Banani (Dhaka)**: North End Coffee Roasters, Crimson Cup, or The Westin Lounge for quiet executive meetings.
                    • **Motijheel / CBD**: InterContinental Lobby Lounge for formal corporate briefings.
                    • **Dhanmondi / Lalmatia**: Gloria Jean's Coffees or Columbus Coffee for casual networking.
                    
                    💡 Tap the Google Maps buttons below to open instant directions and live routing.
                    """.trimIndent()
                }
            }

            // 6. DIRECT CONTACT MATCH
            matchedCards.isNotEmpty() -> {
                val card = matchedCards.first()
                recipientCard = card
                if (isBangla) {
                    """
                    🎯 **কন্টাক্ট পাওয়া গেছে:** ${card.fullName}
                    • **পদবী:** ${card.jobTitle.ifBlank { "উল্লেখ নেই" }}
                    • **প্রতিষ্ঠান:** ${card.company.ifBlank { "উল্লেখ নেই" }}
                    • **মোবাইল:** ${card.phone.ifBlank { "নেই" }}
                    • **ইমেইল:** ${card.email.ifBlank { "নেই" }}
                    • **ঠিকানা:** ${card.address.ifBlank { "সংরক্ষিত নেই" }}
                    • **ক্যাটাগরি:** ${card.category}
                    
                    💡 আপনি নিচের অ্যাকশন কার্ডে সরাসরি কল, WhatsApp, ইমেইল বা কার্ড বিস্তারিত দেখতে পারেন।
                    """.trimIndent()
                } else {
                    """
                    🎯 **Contact Located:** ${card.fullName}
                    • **Title:** ${card.jobTitle.ifBlank { "N/A" }}
                    • **Company:** ${card.company.ifBlank { "N/A" }}
                    • **Phone:** ${card.phone.ifBlank { "N/A" }}
                    • **Email:** ${card.email.ifBlank { "N/A" }}
                    • **Address:** ${card.address.ifBlank { "N/A" }}
                    • **Category:** ${card.category}
                    
                    💡 Use the interactive action card below to directly Call, WhatsApp, Email, or view full details.
                    """.trimIndent()
                }
            }

            // 7. EXECUTIVE STRATEGY
            persona.type == ChatPersonaType.EXECUTIVE_ADVISOR -> {
                if (isBangla) {
                    """
                    🧠 **এক্সিকিউটিভ স্ট্র্যাটেজি ও নেটওয়ার্কিং গাইড:**
                    
                    আপনার ${cards.size} টি বিজনেস কন্টাক্টের নেটওয়ার্ককে সর্বোচ্চ কার্যকর করার ৩টি পদক্ষেপ:
                    1. **কী-অ্যাকাউন্ট ভ্যালু ক্লাস্টারিং**: শীর্ষ পার্টনার ও ক্লায়েন্টদের জন্য নিয়মিত কোয়ার্টারলি টাচপয়েন্ট নির্ধারণ করুন।
                    2. **পার্টনারশিপ ভ্যালু পিচ**: ক্রস-ইন্ডাস্ট্রি সুযোগ খুঁজে যৌথ প্রজেক্ট প্রস্তাব করুন।
                    3. **রিস্ক মিটিগেশন**: একক কন্টাক্টের উপর নির্ভর না করে প্রতিষ্ঠানে একাধিক ব্যক্তির সাথে সংযোগ তৈরি করুন।
                    """.trimIndent()
                } else {
                    """
                    🧠 **Executive Networking & Deal Framework:**
                    
                    Strategic 3-step action roadmap for your ${cards.size} contacts:
                    1. **Tier-1 Account Engagement**: Schedule proactive value-add check-ins with high-value partners.
                    2. **Synergy Pitching**: Identify cross-selling and strategic vendor partnership opportunities.
                    3. **Multi-Threaded Relationships**: Build rapport with multiple key stakeholders in each enterprise.
                    """.trimIndent()
                }
            }

            // 8. GENERAL GREETING & GUIDANCE
            else -> {
                if (isBangla) {
                    """
                    🤖 **CardMate AI সহকারী:**
                    
                    আপনার সংরক্ষিত **${cards.size} টি ভিজিটিং কার্ড** এবং নেটওয়ার্ক ডেটাবেজ প্রস্তুত রয়েছে।
                    
                    আমি আপনাকে সাহায্য করতে পারি:
                    • 📊 *"কার্ড ডিরেক্টরির সামারি দাও"*
                    • ✍️ *"মিটিংয়ের পর ফলো-আপ মেসেজ লিখে দাও"*
                    • 🔍 *"অমুকের ফোন নম্বর বা ইমেইল কি?"*
                    • ☕ *"কাছাকাছি মিটিং স্পট বা ক্যাফে খুঁজে দাও"*
                    • 📇 *"ডুপ্লিকেট কার্ড আছে কি না চেক করো"*
                    """.trimIndent()
                } else {
                    """
                    🤖 **CardMate AI Assistant:**
                    
                    Your **${cards.size} saved business cards** and contact database are indexed and ready.
                    
                    Here are some things I can do for you:
                    • 📊 *"Summarize my business directory and network"*
                    • ✍️ *"Draft a post-meeting follow-up email/WhatsApp"*
                    • 🔍 *"Find contact details for [Person or Company]"*
                    • ☕ *"Find quiet meeting cafes near [Address]"*
                    • 📇 *"Audit duplicate cards and CRM health"*
                    """.trimIndent()
                }
            }
        }

        return ChatMessage(
            role = ChatRole.MODEL,
            text = text,
            modelUsed = persona.defaultModel,
            groundingSources = groundingList,
            isGroundedWithMaps = groundingList.isNotEmpty(),
            matchedCards = matchedCards,
            suggestedFollowUpDraft = followUpDraft,
            draftRecipientCard = recipientCard ?: matchedCards.firstOrNull()
        )
    }
}

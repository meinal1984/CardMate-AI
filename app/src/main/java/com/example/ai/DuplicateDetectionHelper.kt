package com.example.ai

import com.example.data.model.BusinessCard
import kotlin.math.max
import kotlin.math.min

data class DuplicateMatch(
    val cardA: BusinessCard,
    val cardB: BusinessCard,
    val matchScore: Int, // e.g. 92 for 92%
    val matchReasons: List<String>,
    val suggestedMergedCard: BusinessCard
)

object DuplicateDetectionHelper {

    /**
     * Scans a list of business cards and returns all suspected duplicate pairs sorted by match score.
     */
    fun findPotentialDuplicates(cards: List<BusinessCard>, minScore: Int = 65): List<DuplicateMatch> {
        val matches = mutableListOf<DuplicateMatch>()
        val seenPairs = mutableSetOf<String>()

        for (i in 0 until cards.size) {
            for (j in (i + 1) until cards.size) {
                val card1 = cards[i]
                val card2 = cards[j]

                val pairKey = if (card1.id < card2.id) "${card1.id}_${card2.id}" else "${card2.id}_${card1.id}"
                if (seenPairs.contains(pairKey)) continue
                seenPairs.add(pairKey)

                val (score, reasons) = evaluateSimilarity(card1, card2)
                if (score >= minScore) {
                    val merged = mergeTwoCards(card1, card2)
                    matches.add(
                        DuplicateMatch(
                            cardA = card1,
                            cardB = card2,
                            matchScore = score,
                            matchReasons = reasons,
                            suggestedMergedCard = merged
                        )
                    )
                }
            }
        }

        return matches.sortedByDescending { it.matchScore }
    }

    /**
     * Checks a new or scanned card against existing database cards to see if a duplicate already exists.
     */
    fun findBestMatch(newCard: BusinessCard, existingCards: List<BusinessCard>, minScore: Int = 65): DuplicateMatch? {
        var bestMatch: DuplicateMatch? = null
        var highestScore = 0

        for (existing in existingCards) {
            if (newCard.id > 0 && newCard.id == existing.id) continue

            val (score, reasons) = evaluateSimilarity(newCard, existing)
            if (score >= minScore && score > highestScore) {
                highestScore = score
                val merged = mergeTwoCards(existing, newCard)
                bestMatch = DuplicateMatch(
                    cardA = existing,
                    cardB = newCard,
                    matchScore = score,
                    matchReasons = reasons,
                    suggestedMergedCard = merged
                )
            }
        }

        return bestMatch
    }

    /**
     * Evaluates similarity between two cards with fuzzy name matching, company normalization,
     * phone normalization, and email comparison. Returns match score (0-100) and human-readable reasons.
     */
    fun evaluateSimilarity(card1: BusinessCard, card2: BusinessCard): Pair<Int, List<String>> {
        val reasons = mutableListOf<String>()

        val normName1 = normalizeName(card1.fullName)
        val normName2 = normalizeName(card2.fullName)

        val normCompany1 = normalizeCompany(card1.company)
        val normCompany2 = normalizeCompany(card2.company)

        val normPhone1 = normalizePhone(card1.phone)
        val normPhone2 = normalizePhone(card2.phone)
        val normSecPhone1 = normalizePhone(card1.secondaryPhone)
        val normSecPhone2 = normalizePhone(card2.secondaryPhone)

        val normEmail1 = card1.email.trim().lowercase()
        val normEmail2 = card2.email.trim().lowercase()

        var totalWeight = 0.0
        var scoreSum = 0.0

        // 1. Phone Match (High confidence indicator across all resolved phone numbers)
        val phones1 = card1.getAllPhoneNumbers().map { normalizePhone(it) }.filter { it.isNotBlank() }
        val phones2 = card2.getAllPhoneNumbers().map { normalizePhone(it) }.filter { it.isNotBlank() }
        val commonPhones = phones1.intersect(phones2.toSet())
        val phoneMatch = commonPhones.isNotEmpty()

        if (phoneMatch) {
            scoreSum += 95.0 * 3.5
            totalWeight += 3.5
            reasons.add("Matching phone number (${commonPhones.first()})")
        }

        // 2. Email Match (High confidence indicator)
        val emailMatch = normEmail1.isNotBlank() && normEmail2.isNotBlank() && normEmail1 == normEmail2
        if (emailMatch) {
            scoreSum += 98.0 * 3.5
            totalWeight += 3.5
            reasons.add("Exact matching email address ($normEmail1)")
        }

        // 3. Name Similarity (Fuzzy string similarity + token similarity)
        if (normName1.isNotBlank() && normName2.isNotBlank()) {
            val nameSim = stringSimilarity(normName1, normName2)
            scoreSum += nameSim * 4.0
            totalWeight += 4.0

            if (nameSim >= 90) {
                reasons.add("Very high name similarity ('${card1.fullName}' & '${card2.fullName}')")
            } else if (nameSim >= 75) {
                reasons.add("Similar name spelling ('${card1.fullName}' ≈ '${card2.fullName}')")
            }
        }

        // 4. Company Similarity
        if (normCompany1.isNotBlank() && normCompany2.isNotBlank()) {
            val companySim = stringSimilarity(normCompany1, normCompany2)
            scoreSum += companySim * 2.5
            totalWeight += 2.5

            if (companySim >= 85) {
                reasons.add("Matching organization ('${card1.company}' & '${card2.company}')")
            }
        }

        // 5. Job Title Comparison
        val normTitle1 = card1.jobTitle.trim().lowercase()
        val normTitle2 = card2.jobTitle.trim().lowercase()
        if (normTitle1.isNotBlank() && normTitle2.isNotBlank()) {
            val titleSim = stringSimilarity(normTitle1, normTitle2)
            scoreSum += titleSim * 1.0
            totalWeight += 1.0
        }

        if (totalWeight <= 0.0) {
            return Pair(0, emptyList())
        }

        var finalScore = (scoreSum / totalWeight).toInt()

        // Boost if both Name and Company match reasonably well
        if (normName1.isNotBlank() && normName2.isNotBlank() && normCompany1.isNotBlank() && normCompany2.isNotBlank()) {
            val nameSim = stringSimilarity(normName1, normName2)
            val companySim = stringSimilarity(normCompany1, normCompany2)
            if (nameSim >= 80 && companySim >= 80) {
                finalScore = max(finalScore, 90)
                if (!reasons.any { it.contains("name and company") }) {
                    reasons.add(0, "AI identified same person at same company (${nameSim.toInt()}% name & ${companySim.toInt()}% org match)")
                }
            }
        }

        // Clip between 0 and 99
        finalScore = min(99, max(0, finalScore))

        return Pair(finalScore, reasons)
    }

    /**
     * Intelligently merges two cards, keeping primary's ID and combining all non-empty fields,
     * secondary contacts, notes, and photos.
     */
    fun mergeTwoCards(primary: BusinessCard, secondary: BusinessCard): BusinessCard {
        val allMergedPhones = (primary.getAllPhoneNumbers() + secondary.getAllPhoneNumbers())
            .distinctBy { com.example.util.PhoneNumberUtils.normalizeForComparison(it) }

        val mergedPhone = allMergedPhones.firstOrNull() ?: primary.phone.ifBlank { secondary.phone }
        val mergedSecPhone = allMergedPhones.getOrNull(1) ?: ""
        val extraMergedPhones = allMergedPhones.drop(2)

        val mergedEmail = primary.email.ifBlank { secondary.email }
        val mergedWebsite = primary.website.ifBlank { secondary.website }
        val mergedJobTitle = primary.jobTitle.ifBlank { secondary.jobTitle }
        val mergedCompany = primary.company.ifBlank { secondary.company }
        val mergedAddress = primary.address.ifBlank { secondary.address }

        val mergedCategory = if (primary.category.isNotBlank() && primary.category != "Corporate") {
            primary.category
        } else {
            secondary.category.ifBlank { primary.category }
        }

        // Combine notes cleanly
        val combinedNotes = when {
            primary.notes.isBlank() -> secondary.notes
            secondary.notes.isBlank() || secondary.notes == primary.notes -> primary.notes
            primary.notes.contains(secondary.notes) -> primary.notes
            else -> "${primary.notes}\n---\n[Merged Notes]: ${secondary.notes}"
        }

        // Combine social links
        val combinedSocial = when {
            primary.socialLinks.isBlank() -> secondary.socialLinks
            secondary.socialLinks.isBlank() || secondary.socialLinks == primary.socialLinks -> primary.socialLinks
            else -> "${primary.socialLinks} | ${secondary.socialLinks}"
        }

        val frontPhoto = primary.cardFrontImageUri ?: secondary.cardFrontImageUri
        val backPhoto = primary.cardBackImageUri ?: secondary.cardBackImageUri

        val finalSocial = buildList {
            if (combinedSocial.isNotBlank()) add(combinedSocial)
            extraMergedPhones.forEachIndexed { idx, p ->
                if (!combinedSocial.contains(p)) {
                    add("বিকল্প ফোন ${idx + 3}: $p")
                }
            }
        }.joinToString("\n")

        return primary.copy(
            jobTitle = mergedJobTitle,
            company = mergedCompany,
            phone = mergedPhone,
            secondaryPhone = mergedSecPhone,
            email = mergedEmail,
            website = mergedWebsite,
            address = mergedAddress,
            category = mergedCategory,
            notes = combinedNotes,
            socialLinks = finalSocial,
            cardFrontImageUri = frontPhoto,
            cardBackImageUri = backPhoto,
            isFavorite = primary.isFavorite || secondary.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun normalizeName(name: String): String {
        return name.lowercase()
            .replace(".", "")
            .replace(",", "")
            .replace("md ", "")
            .replace("mohammed ", "")
            .replace("muhammad ", "")
            .replace("dr ", "")
            .replace("engr ", "")
            .replace("engineer ", "")
            .replace("mr ", "")
            .replace("mrs ", "")
            .replace("ms ", "")
            .replace("advocate ", "")
            .replace("adv ", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeCompany(company: String): String {
        return company.lowercase()
            .replace(".", "")
            .replace(",", "")
            .replace("limited", "ltd")
            .replace("corporation", "corp")
            .replace("incorporated", "inc")
            .replace("private", "pvt")
            .replace("company", "co")
            .replace("solutions", "sol")
            .replace("technologies", "tech")
            .replace("international", "intl")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
            .removePrefix("880")
            .removePrefix("0")
    }

    private fun stringSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 100.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = max(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        val levSim = ((maxLen - distance).toDouble() / maxLen.toDouble()) * 100.0

        // Token sort similarity for word swaps e.g. "Rahim Ahmed" vs "Ahmed Rahim"
        val tokens1 = s1.split(" ").filter { it.isNotBlank() }.sorted().joinToString(" ")
        val tokens2 = s2.split(" ").filter { it.isNotBlank() }.sorted().joinToString(" ")
        val tokenDistance = levenshteinDistance(tokens1, tokens2)
        val tokenMaxLen = max(tokens1.length, tokens2.length)
        val tokenSim = if (tokenMaxLen > 0) ((tokenMaxLen - tokenDistance).toDouble() / tokenMaxLen.toDouble()) * 100.0 else levSim

        return max(levSim, tokenSim)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}

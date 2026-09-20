package com.example.util

import com.example.data.model.BusinessCard
import java.util.regex.Pattern

/**
 * Utility for parsing, splitting, and managing single or multiple phone numbers.
 * Resolves issues where multiple alternative numbers are concatenated or saved into a single field.
 */
object PhoneNumberUtils {

    // Regex for standard phone numbers (international + local + bangla digits)
    private val PHONE_NUMBER_REGEX = Pattern.compile(
        """(?:\+?[0-9০-৯]{1,4}[\s\-]*)?(?:\(?[0-9০-৯]{2,5}\)?[\s\-]*)?[0-9০-৯]{3,4}[\s\-]?[0-9০-৯]{3,5}"""
    )

    /**
     * Splits any string that may contain one or multiple phone numbers separated by:
     * commas (,), slashes (/ or \), semicolons (;), newlines (\n), vertical bars (|),
     * or words like 'or', 'and', 'অথবা', 'এবং'.
     *
     * Returns a list of clean, distinct, non-empty phone numbers.
     */
    fun splitPhoneNumbers(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        val trimmed = raw.trim()

        // Replace common words/delimiters with a unified delimiter "|"
        val normalized = trimmed
            .replace(Regex("""(?i)\s+(?:or|and|অথবা|এবং)\s+"""), "|")
            .replace("\r\n", "|")
            .replace("\n", "|")
            .replace("\r", "|")
            .replace(";", "|")
            .replace(",", "|")
            .replace("/", "|")
            .replace("\\", "|")

        val tokens = normalized.split("|")
            .map { cleanPhoneToken(it) }
            .filter { it.isNotBlank() }

        // If simple splitting produced tokens that still have multiple numbers joined
        val results = mutableListOf<String>()
        for (token in tokens) {
            val matcher = PHONE_NUMBER_REGEX.matcher(token)
            val matchesInToken = mutableListOf<String>()
            while (matcher.find()) {
                val match = cleanPhoneToken(matcher.group())
                if (match.length >= 6) {
                    matchesInToken.add(match)
                }
            }
            if (matchesInToken.size > 1) {
                results.addAll(matchesInToken)
            } else {
                results.add(token)
            }
        }

        // Deduplicate while preserving order
        val distinct = mutableListOf<String>()
        for (p in results) {
            val normalizedNum = normalizeForComparison(p)
            if (distinct.none { normalizeForComparison(it) == normalizedNum }) {
                distinct.add(p)
            }
        }
        return distinct
    }

    /**
     * Cleans a single phone token by removing field labels (e.g. "Tel:", "Mob:", "ফোন:", etc.)
     */
    fun cleanPhoneToken(token: String): String {
        var res = token.trim()
        val lower = res.lowercase()
        val prefixes = listOf(
            "secondary phone:", "secondary:", "primary phone:", "primary:",
            "phone:", "mobile:", "tel:", "telephone:", "cell:", "contact:", "fax:",
            "ফোন:", "মোবাইল:", "টেলিফোন:", "সেল:", "যোগাযোগ:", "বিকল্প ফোন:", "বিকল্প:",
            "phone", "mobile", "tel", "cell", "ফোন", "মোবাইল"
        )
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                res = res.substring(prefix.length).trim()
                if (res.startsWith(":")) res = res.substring(1).trim()
                break
            }
        }
        return res
    }

    fun normalizeForComparison(phone: String): String {
        return phone.replace(Regex("""[\s\-\(\)\+]"""), "")
    }

    /**
     * Given an edited value at `index` in a list of phone numbers:
     * If the user pasted or entered multiple numbers separated by delimiters,
     * splits it and returns a new list with multiple separate fields.
     */
    fun handlePhoneListEdit(
        currentList: List<String>,
        index: Int,
        newVal: String
    ): List<String> {
        val split = splitPhoneNumbers(newVal)
        if (split.size > 1) {
            val mutable = currentList.toMutableList()
            if (index in mutable.indices) {
                // Replace current field with first number
                mutable[index] = split[0]
                // Insert remaining numbers as new fields right after
                mutable.addAll(index + 1, split.drop(1))
            } else {
                mutable.addAll(split)
            }
            return mutable
        } else {
            val mutable = currentList.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = newVal
            } else {
                mutable.add(newVal)
            }
            return mutable
        }
    }

    /**
     * Extracts all distinct phone numbers from a BusinessCard.
     */
    fun extractAllCardPhones(card: BusinessCard): List<String> {
        val list = mutableListOf<String>()
        list.addAll(splitPhoneNumbers(card.phone))
        list.addAll(splitPhoneNumbers(card.secondaryPhone))

        if (card.socialLinks.isNotBlank()) {
            card.socialLinks.split("\n", "|").forEach { part ->
                val trimmed = part.trim()
                val lower = trimmed.lowercase()
                if (lower.contains("phone") || lower.contains("mobile") ||
                    lower.contains("tel") || lower.contains("ফোন") ||
                    lower.contains("মোবাইল") || lower.contains("বিকল্প")
                ) {
                    if (trimmed.contains(":")) {
                        val v = trimmed.split(":", limit = 2)[1].trim()
                        list.addAll(splitPhoneNumbers(v))
                    } else {
                        list.addAll(splitPhoneNumbers(trimmed))
                    }
                }
            }
        }

        // Deduplicate
        val result = mutableListOf<String>()
        for (item in list) {
            val norm = normalizeForComparison(item)
            if (norm.isNotBlank() && result.none { normalizeForComparison(it) == norm }) {
                result.add(item)
            }
        }
        return result
    }

    /**
     * Extracts all secondary/alternative phone numbers from a BusinessCard (excluding primary phone).
     */
    fun extractSecondaryPhones(card: BusinessCard): List<String> {
        val primaryNorm = normalizeForComparison(card.phone)
        return extractAllCardPhones(card).filter {
            normalizeForComparison(it) != primaryNorm
        }
    }
}

package com.example.data.model

import java.util.UUID

data class ProfileFieldItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Mobile", // "Mobile", "Work", "WhatsApp", "Home", "Fax", "Other", etc.
    val value: String = ""
)

data class UserProfile(
    val fullName: String = "Mrinal Kanti Roy",
    val jobTitle: String = "Senior Electrical & AI Engineer",
    val company: String = "CardMate Innovations Ltd.",
    val department: String = "AI & Research Division",
    val phone: String = "+8801719205945",
    val secondaryPhone: String = "+880 1912-345678",
    val email: String = "mrinal.eee@gmail.com",
    val website: String = "https://cardmate.ai",
    val address: String = "Gulshan-2, Dhaka 1212, Bangladesh",
    val category: String = "Tech & IT",
    val bio: String = "Passionate about AI architectures, embedded hardware, mobile innovation, and digital networking solutions.",
    val socialLinks: String = "WhatsApp: +8801719205945 | LinkedIn: linkedin.com/in/mrinal-eee | GitHub: github.com/mrinal",
    val photoUri: String? = null,
    val companyLogoUri: String? = null,
    val cardLayoutTemplate: String = "modern_slate",
    // Multi-entry collections
    val phoneList: List<ProfileFieldItem> = emptyList(),
    val emailList: List<ProfileFieldItem> = emptyList(),
    val websiteList: List<ProfileFieldItem> = emptyList(),
    val socialList: List<ProfileFieldItem> = emptyList()
) {
    /**
     * Resolves all phone entries, ensuring fallback to scalar phone & secondaryPhone if list is empty.
     */
    fun getResolvedPhones(): List<ProfileFieldItem> {
        if (phoneList.isNotEmpty()) {
            return phoneList.filter { it.value.isNotBlank() }
        }
        val list = mutableListOf<ProfileFieldItem>()
        if (phone.isNotBlank()) list.add(ProfileFieldItem(label = "Mobile", value = phone))
        if (secondaryPhone.isNotBlank()) list.add(ProfileFieldItem(label = "Work", value = secondaryPhone))
        return list
    }

    /**
     * Resolves all email entries, ensuring fallback to scalar email if list is empty.
     */
    fun getResolvedEmails(): List<ProfileFieldItem> {
        if (emailList.isNotEmpty()) {
            return emailList.filter { it.value.isNotBlank() }
        }
        if (email.isNotBlank()) return listOf(ProfileFieldItem(label = "Work", value = email))
        return emptyList()
    }

    /**
     * Resolves all website entries.
     */
    fun getResolvedWebsites(): List<ProfileFieldItem> {
        if (websiteList.isNotEmpty()) {
            return websiteList.filter { it.value.isNotBlank() }
        }
        if (website.isNotBlank()) return listOf(ProfileFieldItem(label = "Company", value = website))
        return emptyList()
    }

    /**
     * Resolves all social media / messaging entries.
     */
    fun getResolvedSocials(): List<ProfileFieldItem> {
        if (socialList.isNotEmpty()) {
            return socialList.filter { it.value.isNotBlank() }
        }
        if (socialLinks.isNotBlank()) {
            val parts = socialLinks.split("|")
            return parts.mapNotNull { part ->
                val p = part.trim()
                if (p.isNotBlank()) {
                    if (p.contains(":")) {
                        val sp = p.split(":", limit = 2)
                        ProfileFieldItem(label = sp[0].trim(), value = sp[1].trim())
                    } else {
                        ProfileFieldItem(label = "Social", value = p)
                    }
                } else null
            }
        }
        return emptyList()
    }

    fun toBusinessCard(): BusinessCard {
        val resolvedPhones = getResolvedPhones()
        val primaryP = resolvedPhones.firstOrNull()?.value ?: phone
        val secondaryP = resolvedPhones.getOrNull(1)?.value ?: secondaryPhone
        val resolvedEmails = getResolvedEmails()
        val primaryE = resolvedEmails.firstOrNull()?.value ?: email
        val resolvedWebs = getResolvedWebsites()
        val primaryW = resolvedWebs.firstOrNull()?.value ?: website
        val resolvedSoc = getResolvedSocials()
        val formattedSoc = if (resolvedSoc.isNotEmpty()) {
            resolvedSoc.joinToString(" | ") { "${it.label}: ${it.value}" }
        } else {
            socialLinks
        }

        return BusinessCard(
            id = 999999L,
            fullName = fullName,
            jobTitle = jobTitle,
            company = company,
            phone = primaryP,
            secondaryPhone = secondaryP,
            email = primaryE,
            website = primaryW,
            address = address,
            category = category,
            notes = bio,
            socialLinks = formattedSoc,
            cardFrontImageUri = photoUri,
            cardLayoutTemplate = cardLayoutTemplate,
            isFavorite = true,
            isBackedUpToCloud = true
        )
    }
}

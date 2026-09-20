package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a scanned or manually created business card.
 */
@Entity(tableName = "business_cards")
data class BusinessCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phone: String = "",
    val secondaryPhone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val category: String = "Corporate", // Corporate, Tech, Medical, Finance, Clients, Partners, Vendors, Personal, Other
    val notes: String = "",
    val socialLinks: String = "", // e.g. LinkedIn, Twitter, GitHub
    val cardFrontImageUri: String? = null,
    val cardBackImageUri: String? = null,
    val cardLayoutTemplate: String = "modern_slate", // modern_slate, executive_gold, cyber_neon, minimal_clean, ocean_gradient, vintage_kraft
    val cardWidthMm: Float = 88.9f, // Default 3.5 in = 88.9 mm
    val cardHeightMm: Float = 50.8f, // Default 2.0 in = 50.8 mm
    val cardStandardName: String = "Standard US (3.5\" × 2.0\")",
    val isFavorite: Boolean = false,
    val isSyncedWithGoogleContacts: Boolean = false,
    val isBackedUpToCloud: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val rawOcrText: String = "",
    val language: String = "auto",
    // --- Card Design & Customization Preferences (Persisted in Room Database) ---
    val customPrimaryBgColor: Long? = null,
    val customSecondaryBgColor: Long? = null,
    val customAccentColor: Long? = null,
    val customTextColor: Long? = null,
    val fontFamilyType: String = "sans_serif", // sans_serif, serif, monospace, cursive, condensed
    val layoutStyle: String = "modern_floating", // modern_floating, executive_classic, minimalist_clean, cyber_badge, split_duotone, vertical_showcase
    val bgPattern: String = "gradient", // gradient, solid, mesh, dots, stripes, geometric
    val cornerRadiusDp: Int = 18, // 0, 8, 16, 18, 22, 28
    val borderStyle: String = "subtle", // none, subtle, bold, glow
    val textAlignment: String = "left", // left, center, right
    val showQrBadge: Boolean = true,
    val showNfcBadge: Boolean = true,
    val showAvatar: Boolean = true,
    val showCategoryBadge: Boolean = true
) {
    val displaySubtitle: String
        get() = when {
            jobTitle.isNotBlank() && company.isNotBlank() -> "$jobTitle • $company"
            jobTitle.isNotBlank() -> jobTitle
            company.isNotBlank() -> company
            else -> category
        }

    val primaryContact: String
        get() = phone.ifBlank { email.ifBlank { website } }

    /** Returns all resolved phone numbers, splitting any combined entries */
    fun getAllPhoneNumbers(): List<String> = com.example.util.PhoneNumberUtils.extractAllCardPhones(this)

    /** Returns all resolved secondary/alternative phone numbers */
    fun getSecondaryPhoneNumbers(): List<String> = com.example.util.PhoneNumberUtils.extractSecondaryPhones(this)

    /** Returns effective primary background color considering custom selection or template default */
    val effectivePrimaryBgColor: Long
        get() = customPrimaryBgColor ?: CardTemplate.fromId(cardLayoutTemplate).primaryBgColor

    /** Returns effective secondary background color considering custom selection or template default */
    val effectiveSecondaryBgColor: Long
        get() = customSecondaryBgColor ?: CardTemplate.fromId(cardLayoutTemplate).secondaryBgColor

    /** Returns effective accent color considering custom selection or template default */
    val effectiveAccentColor: Long
        get() = customAccentColor ?: CardTemplate.fromId(cardLayoutTemplate).accentColor

    /** Returns effective text color considering custom selection or template default */
    val effectiveTextColor: Long
        get() = customTextColor ?: CardTemplate.fromId(cardLayoutTemplate).textColor
}

enum class CardFontFamily(val id: String, val displayNameEn: String, val displayNameBn: String) {
    SANS_SERIF("sans_serif", "Modern Sans", "মডার্ন স্যান্স"),
    SERIF("serif", "Classic Serif", "ক্লাসিক সেরিফ"),
    MONOSPACE("monospace", "Tech Monospace", "টেক মনোস্পেস"),
    CURSIVE("cursive", "Boutique Cursive", "হ্যান্ডরাইটিং কার্সিভ"),
    CONDENSED("condensed", "Compact Modern", "কমপ্যাক্ট মডার্ন");

    companion object {
        fun fromId(id: String): CardFontFamily = entries.find { it.id == id } ?: SANS_SERIF
    }
}

enum class CardLayoutStyle(
    val id: String,
    val displayNameEn: String,
    val displayNameBn: String,
    val descriptionEn: String,
    val descriptionBn: String
) {
    MODERN_FLOATING(
        "modern_floating",
        "Modern Floating",
        "মডার্ন ফ্লোটিং",
        "Asymmetric contemporary layout with rounded badges and sleek contact hierarchy",
        "রাউন্ডেড ব্যাজ এবং আধুনিক লুক সহ ব্যালেন্সড লেআউট"
    ),
    EXECUTIVE_CLASSIC(
        "executive_classic",
        "Executive Classic",
        "এক্সিকিউটিভ ক্লাসিক",
        "Symmetrical luxury corporate aesthetic with centered crest & divider lines",
        "মাঝখানে ফোকাসড এবং ফরমাল কর্পোরেট গোল্ডেন স্টাইল"
    ),
    MINIMALIST_CLEAN(
        "minimalist_clean",
        "Minimalist Pure",
        "মিনিমালিস্ট পিওর",
        "Ultra clean high-contrast architecture with airy negative space",
        "হাই-কনট্রাস্ট এবং পরিষ্কার মিনিমাল ডিজাইন"
    ),
    CYBER_BADGE(
        "cyber_badge",
        "Cyberpunk Tech",
        "সাইবারপাঙ্ক টেক",
        "Futuristic neon frame with monospace telemetry and tech corner chips",
        "ভবিষ্যতমুখী নিয়ন বর্ডার এবং টেকনোলজি স্টাইল"
    ),
    SPLIT_DUOTONE(
        "split_duotone",
        "Split Duo-Tone",
        "স্প্লিট ডুও-টোন",
        "Contrasting left accent block with distinct profile column & contact body",
        "বামপাশে অ্যাকসেন্ট কালার ব্লক এবং ডানপাশে বিস্তারিত তথ্য"
    ),
    VERTICAL_SHOWCASE(
        "vertical_showcase",
        "Vertical Showcase",
        "ভার্টিক্যাল শোকেস",
        "Centered executive banner design with prominent identity accents",
        "বড় ব্র্যান্ড ও পরিচয় প্রদর্শনকারী নান্দনিক রূপ"
    );

    companion object {
        fun fromId(id: String): CardLayoutStyle = entries.find { it.id == id } ?: MODERN_FLOATING
    }
}

enum class CardBgPattern(val id: String, val displayNameEn: String, val displayNameBn: String) {
    GRADIENT("gradient", "Smooth Gradient", "স্মুথ গ্রেডিয়েন্ট"),
    SOLID("solid", "Luxury Solid", "সলিড লাক্সারি"),
    MESH("mesh", "Aurora Mesh", "অরোরা মেশ"),
    DOTS("dots", "Subtle Dot Grid", "ডট গ্রিড"),
    STRIPES("stripes", "Tech Stripes", "টেক স্ট্রাইপস"),
    GEOMETRIC("geometric", "Geometric Hex", "জ্যামিতিক অ্যাকসেন্ট");

    companion object {
        fun fromId(id: String): CardBgPattern = entries.find { it.id == id } ?: GRADIENT
    }
}

enum class CardCategory(val englishName: String, val banglaName: String, val colorHex: Long) {
    TECH("Tech & IT", "তথ্যপ্রযুক্তি", 0xFF38BDF8),
    CORPORATE("Corporate", "কর্পোরেট", 0xFF818CF8),
    CLIENTS("Clients", "ক্লায়েন্ট", 0xFF34D399),
    FINANCE("Finance & Banking", "ব্যাংকিং ও ফাইন্যান্স", 0xFFFBBF24),
    MEDICAL("Healthcare & Medical", "স্বাস্থ্য ও চিকিৎসা", 0xFFF87171),
    PARTNERS("Partners", "পার্টনার", 0xFFA78BFA),
    VENDORS("Vendors", "ভেন্ডর", 0xFFFB923C),
    PERSONAL("Personal", "ব্যক্তিগত", 0xFFEC4899),
    OTHER("Other", "অন্যান্য", 0xFF94A3B8);

    companion object {
        fun fromString(name: String): CardCategory {
            return entries.find { 
                it.name.equals(name, ignoreCase = true) || 
                it.englishName.equals(name, ignoreCase = true) ||
                it.banglaName.equals(name, ignoreCase = true)
            } ?: CORPORATE
        }
    }
}

enum class CardTemplate(
    val id: String,
    val displayNameEn: String,
    val displayNameBn: String,
    val primaryBgColor: Long,
    val secondaryBgColor: Long,
    val accentColor: Long,
    val textColor: Long
) {
    MODERN_SLATE("modern_slate", "Modern Slate", "মডার্ন স্লেট", 0xFF0F172A, 0xFF1E293B, 0xFF2DD4BF, 0xFFF8FAFC),
    PRISTINE_WHITE("pristine_white", "Pristine White", "হোয়াইট মিনিমাল", 0xFFFFFFFF, 0xFFF1F5F9, 0xFF0284C7, 0xFF0F172A),
    ROYAL_CREAM("royal_cream", "Ivory Elegance", "আইভরি গোল্ড", 0xFFFFFBEB, 0xFFFEF3C7, 0xFFB45309, 0xFF1C1917),
    EXECUTIVE_GOLD("executive_gold", "Executive Gold", "এক্সিকিউটিভ গোল্ড", 0xFF18181B, 0xFF27272A, 0xFFEAB308, 0xFFFEF08A),
    MIDNIGHT_NAVY("midnight_navy", "Midnight Navy", "মিডনাইট নেভি", 0xFF0A192F, 0xFF172A45, 0xFF38BDF8, 0xFFCCD6F6),
    CYBER_NEON("cyber_neon", "Cyber Neon", "সাইবার নিয়ন", 0xFF1E1B4B, 0xFF312E81, 0xFF38BDF8, 0xFFE0E7FF),
    MINIMAL_CLEAN("minimal_clean", "Minimal Pure", "মিনিমাল পিওর", 0xFF111827, 0xFF1F2937, 0xFFF43F5E, 0xFFFFFFFF),
    OCEAN_GRADIENT("ocean_gradient", "Ocean Breeze", "ওশান ব্রিজ", 0xFF042F2E, 0xFF115E59, 0xFF5EEAD4, 0xFFCCFBF1),
    VINTAGE_KRAFT("vintage_kraft", "Executive Bronze", "ব্রোঞ্জ প্রিমিয়াম", 0xFF292524, 0xFF44403C, 0xFFFB923C, 0xFFFAFAF9);

    companion object {
        fun fromId(id: String): CardTemplate {
            return entries.find { it.id == id } ?: MODERN_SLATE
        }
    }
}

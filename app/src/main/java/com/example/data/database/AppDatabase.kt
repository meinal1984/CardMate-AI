package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BusinessCardDao
import com.example.data.dao.CardDesignPresetDao
import com.example.data.model.BusinessCard
import com.example.data.model.CardDesignPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [BusinessCard::class, CardDesignPreset::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao
    abstract fun cardDesignPresetDao(): CardDesignPresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardmate_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                scope.launch(Dispatchers.IO) {
                    try {
                        val cardDao = instance.businessCardDao()
                        val existing = cardDao.getCardById(1)
                        if (existing == null) {
                            populateInitialCards(cardDao)
                        }

                        val presetDao = instance.cardDesignPresetDao()
                        if (presetDao.getPresetCount() == 0) {
                            populateInitialPresets(presetDao)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return getDatabase(context)
        }

        suspend fun populateInitialPresets(presetDao: CardDesignPresetDao) {
            val starterPresets = listOf(
                CardDesignPreset(
                    name = "Midnight Obsidian",
                    primaryBgColor = 0xFF0F172A,
                    secondaryBgColor = 0xFF1E293B,
                    accentColor = 0xFF2DD4BF,
                    textColor = 0xFFF8FAFC,
                    fontFamilyType = "sans_serif",
                    layoutStyle = "modern_floating",
                    bgPattern = "gradient",
                    cornerRadiusDp = 18,
                    borderStyle = "subtle",
                    textAlignment = "left",
                    isUserCreated = false
                ),
                CardDesignPreset(
                    name = "Executive Gold",
                    primaryBgColor = 0xFF18181B,
                    secondaryBgColor = 0xFF27272A,
                    accentColor = 0xFFEAB308,
                    textColor = 0xFFFEF08A,
                    fontFamilyType = "serif",
                    layoutStyle = "executive_classic",
                    bgPattern = "gradient",
                    cornerRadiusDp = 16,
                    borderStyle = "bold",
                    textAlignment = "center",
                    isUserCreated = false
                ),
                CardDesignPreset(
                    name = "Cyberpunk Neon",
                    primaryBgColor = 0xFF0B0F19,
                    secondaryBgColor = 0xFF1E1B4B,
                    accentColor = 0xFF38BDF8,
                    textColor = 0xFFE0E7FF,
                    fontFamilyType = "monospace",
                    layoutStyle = "cyber_badge",
                    bgPattern = "dots",
                    cornerRadiusDp = 8,
                    borderStyle = "glow",
                    textAlignment = "left",
                    isUserCreated = false
                ),
                CardDesignPreset(
                    name = "Emerald Forest",
                    primaryBgColor = 0xFF042F2E,
                    secondaryBgColor = 0xFF134E4A,
                    accentColor = 0xFF34D399,
                    textColor = 0xFFECFDF5,
                    fontFamilyType = "sans_serif",
                    layoutStyle = "split_duotone",
                    bgPattern = "mesh",
                    cornerRadiusDp = 20,
                    borderStyle = "subtle",
                    textAlignment = "left",
                    isUserCreated = false
                ),
                CardDesignPreset(
                    name = "Minimalist Luxe",
                    primaryBgColor = 0xFF18181B,
                    secondaryBgColor = 0xFF121214,
                    accentColor = 0xFFF43F5E,
                    textColor = 0xFFFFFFFF,
                    fontFamilyType = "condensed",
                    layoutStyle = "minimalist_clean",
                    bgPattern = "solid",
                    cornerRadiusDp = 14,
                    borderStyle = "subtle",
                    textAlignment = "left",
                    isUserCreated = false
                ),
                CardDesignPreset(
                    name = "Royal Velvet",
                    primaryBgColor = 0xFF3B0764,
                    secondaryBgColor = 0xFF581C87,
                    accentColor = 0xFFC084FC,
                    textColor = 0xFFFAF5FF,
                    fontFamilyType = "cursive",
                    layoutStyle = "vertical_showcase",
                    bgPattern = "gradient",
                    cornerRadiusDp = 22,
                    borderStyle = "subtle",
                    textAlignment = "center",
                    isUserCreated = false
                )
            )
            presetDao.insertPresets(starterPresets)
        }

        suspend fun populateInitialCards(dao: BusinessCardDao) {
                val sampleCards = listOf(
                    BusinessCard(
                        id = 1,
                        fullName = "Mrinal Kanti Roy",
                        jobTitle = "Chief Technology Officer",
                        company = "NeuralSphere AI Ltd.",
                        phone = "+8801719205945",
                        secondaryPhone = "+880 1819-987654",
                        email = "mrinal.eee@gmail.com",
                        website = "https://neuralsphere.ai",
                        address = "Plot 42, Gulshan-2, Dhaka 1212, Bangladesh",
                        category = "Tech & IT",
                        notes = "Met at AI Summit 2026. Discussion regarding enterprise computer vision & mobile ML.",
                        socialLinks = "WhatsApp: @mrinal.eee | linkedin.com/in/mrinal-ai",
                        cardLayoutTemplate = "modern_slate",
                        cardWidthMm = 88.9f,
                        cardHeightMm = 50.8f,
                        cardStandardName = "Standard US (3.5\" × 2.0\")",
                        isFavorite = true,
                        isSyncedWithGoogleContacts = true,
                        isBackedUpToCloud = true,
                        createdAt = System.currentTimeMillis() - 86400000 * 2,
                        rawOcrText = "Mrinal Kanti Roy\nCTO | NeuralSphere AI Ltd.\nPhone: +8801719205945\nWhatsApp: @mrinal.eee\nmrinal.eee@gmail.com\nDhaka, Bangladesh",
                        language = "en"
                    ),
                    BusinessCard(
                        id = 2,
                        fullName = "ড. আনিকা তাসনিম (Dr. Anika Tasnim)",
                        jobTitle = "কার্ডিওলজিস্ট ও সহযোগী অধ্যাপক",
                        company = "ঢাকা স্পেশালাইজড কার্ডিয়াক কেয়ার সেন্টার",
                        phone = "+880 1911-223344",
                        secondaryPhone = "+880 2-9876543",
                        email = "dr.anika@dcardiac.org",
                        website = "https://dcardiac.org",
                        address = "রোড ৭, ধানমন্ডি, ঢাকা ১২০৫",
                        category = "Healthcare & Medical",
                        notes = "কনসাল্টেশন সময়: শনি-বুধ বিকাল ৫টা থেকে রাত ৯টা।",
                        socialLinks = "facebook.com/dranikatasnim",
                        cardLayoutTemplate = "executive_gold",
                        cardWidthMm = 85.6f,
                        cardHeightMm = 53.98f,
                        cardStandardName = "Credit Card Standard (85.6 × 54.0 mm)",
                        isFavorite = true,
                        isSyncedWithGoogleContacts = false,
                        isBackedUpToCloud = true,
                        createdAt = System.currentTimeMillis() - 86400000 * 4,
                        rawOcrText = "ড. আনিকা তাসনিম\nকার্ডিওলজিস্ট ও সহযোগী অধ্যাপক\nঢাকা স্পেশালাইজড কার্ডিয়াক কেয়ার সেন্টার\n+880 1911-223344",
                        language = "bn"
                    ),
                    BusinessCard(
                        id = 3,
                        fullName = "Sarah Jenkins",
                        jobTitle = "Head of Global Partnerships",
                        company = "Vertex Venture Capital",
                        phone = "+1 (415) 890-3421",
                        secondaryPhone = "",
                        email = "sarah.j@vertexvc.com",
                        website = "https://vertexvc.com",
                        address = "500 Howard Street, Suite 400, San Francisco, CA 94105",
                        category = "Finance & Banking",
                        notes = "Interested in Series-A AI startups across South Asia and Southeast Asia.",
                        socialLinks = "twitter.com/sarah_vertex",
                        cardLayoutTemplate = "cyber_neon",
                        cardWidthMm = 88.9f,
                        cardHeightMm = 50.8f,
                        cardStandardName = "Standard US (3.5\" × 2.0\")",
                        isFavorite = false,
                        isSyncedWithGoogleContacts = true,
                        isBackedUpToCloud = true,
                        createdAt = System.currentTimeMillis() - 86400000 * 6,
                        rawOcrText = "Sarah Jenkins\nHead of Partnerships\nVertex Venture Capital\nsarah.j@vertexvc.com",
                        language = "en"
                    ),
                    BusinessCard(
                        id = 4,
                        fullName = "রাকিবুল হাসান (Rakibul Hasan)",
                        jobTitle = "ব্যবস্থাপনা পরিচালক (Managing Director)",
                        company = "মেঘনা ইম্পেক্স অ্যান্ড লজিস্টিকস",
                        phone = "+880 1812-778899",
                        secondaryPhone = "+880 31-654321",
                        email = "rakib@meghnalogistics.com.bd",
                        website = "https://meghnalogistics.com.bd",
                        address = "আগ্রাবাদ বাণিজ্যিক এলাকা, চট্টগ্রাম ৪১০০",
                        category = "Corporate",
                        notes = "আন্তর্জাতিক কার্গো হ্যান্ডলিং এবং সিএন্ডএফ পার্টনারশিপ সংক্রান্ত।",
                        socialLinks = "",
                        cardLayoutTemplate = "ocean_gradient",
                        cardWidthMm = 90.0f,
                        cardHeightMm = 50.0f,
                        cardStandardName = "European Standard (90 × 50 mm)",
                        isFavorite = false,
                        isSyncedWithGoogleContacts = false,
                        isBackedUpToCloud = false,
                        createdAt = System.currentTimeMillis() - 86400000 * 8,
                        rawOcrText = "রাকিবুল হাসান\nব্যবস্থাপনা পরিচালক\nমেঘনা ইম্পেক্স অ্যান্ড লজিস্টিকস\nচট্টগ্রাম",
                        language = "bn"
                    ),
                    BusinessCard(
                        id = 5,
                        fullName = "Elena Rostova",
                        jobTitle = "Lead UX & Design Architect",
                        company = "Studio Lumina Interactive",
                        phone = "+44 20 7946 0912",
                        secondaryPhone = "",
                        email = "elena@studiolumina.design",
                        website = "https://studiolumina.design",
                        address = "24 Shoreditch High St, London E1 6PG, United Kingdom",
                        category = "Clients",
                        notes = "Collaborating on brand design systems and dark-mode component styling.",
                        socialLinks = "dribbble.com/elenarostova",
                        cardLayoutTemplate = "minimal_clean",
                        cardWidthMm = 85.0f,
                        cardHeightMm = 55.0f,
                        cardStandardName = "ISO Standard (85 × 55 mm)",
                        isFavorite = true,
                        isSyncedWithGoogleContacts = false,
                        isBackedUpToCloud = true,
                        createdAt = System.currentTimeMillis() - 86400000 * 10,
                        rawOcrText = "Elena Rostova\nLead UX Architect\nStudio Lumina Interactive\nelena@studiolumina.design",
                        language = "en"
                    )
                )
                dao.insertCards(sampleCards)
            }
    }
}

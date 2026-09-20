package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for saved virtual card design presets & themes.
 */
@Entity(tableName = "card_design_presets")
data class CardDesignPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val primaryBgColor: Long,
    val secondaryBgColor: Long,
    val accentColor: Long,
    val textColor: Long,
    val fontFamilyType: String = "sans_serif",
    val layoutStyle: String = "modern_floating",
    val bgPattern: String = "gradient",
    val cornerRadiusDp: Int = 18,
    val borderStyle: String = "subtle",
    val textAlignment: String = "left",
    val showQrBadge: Boolean = true,
    val showNfcBadge: Boolean = true,
    val isUserCreated: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

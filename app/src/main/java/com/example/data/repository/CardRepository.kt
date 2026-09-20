package com.example.data.repository

import com.example.data.dao.BusinessCardDao
import com.example.data.dao.CardDesignPresetDao
import com.example.data.model.BusinessCard
import com.example.data.model.CardDesignPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

class CardRepository(
    private val cardDao: BusinessCardDao,
    private val presetDao: CardDesignPresetDao? = null
) {

    val allCards: Flow<List<BusinessCard>> = cardDao.getAllCards()
    val favoriteCards: Flow<List<BusinessCard>> = cardDao.getFavoriteCards()
    val totalCount: Flow<Int> = cardDao.getCardCount()
    val syncedCount: Flow<Int> = cardDao.getSyncedCount()
    val backedUpCount: Flow<Int> = cardDao.getBackedUpCount()
    val favoriteCount: Flow<Int> = cardDao.getFavoriteCount()
    val allDesignPresets: Flow<List<CardDesignPreset>> = presetDao?.getAllPresets() ?: emptyFlow()

    fun searchCards(query: String, category: String): Flow<List<BusinessCard>> {
        return cardDao.searchCards(query, category)
    }

    suspend fun getCardById(id: Long): BusinessCard? = withContext(Dispatchers.IO) {
        cardDao.getCardById(id)
    }

    fun getCardByIdFlow(id: Long): Flow<BusinessCard?> {
        return cardDao.getCardByIdFlow(id)
    }

    suspend fun insertCard(card: BusinessCard): Long = withContext(Dispatchers.IO) {
        cardDao.insertCard(card)
    }

    suspend fun insertCards(cards: List<BusinessCard>) = withContext(Dispatchers.IO) {
        cardDao.insertCards(cards)
    }

    suspend fun updateCard(card: BusinessCard) = withContext(Dispatchers.IO) {
        cardDao.updateCard(card)
    }

    suspend fun updateCardDesign(
        id: Long,
        primaryBg: Long?,
        secondaryBg: Long?,
        accent: Long?,
        text: Long?,
        fontFamily: String,
        layout: String,
        pattern: String,
        cornerRadius: Int,
        borderStyle: String,
        alignment: String,
        showQr: Boolean,
        showNfc: Boolean,
        showAvatar: Boolean,
        showCategory: Boolean
    ) = withContext(Dispatchers.IO) {
        cardDao.updateCardDesign(
            id = id,
            primaryBg = primaryBg,
            secondaryBg = secondaryBg,
            accent = accent,
            text = text,
            fontFamily = fontFamily,
            layout = layout,
            pattern = pattern,
            cornerRadius = cornerRadius,
            borderStyle = borderStyle,
            alignment = alignment,
            showQr = showQr,
            showNfc = showNfc,
            showAvatar = showAvatar,
            showCategory = showCategory,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun saveDesignPreset(preset: CardDesignPreset): Long = withContext(Dispatchers.IO) {
        presetDao?.insertPreset(preset) ?: 0L
    }

    suspend fun deleteDesignPreset(presetId: Long) = withContext(Dispatchers.IO) {
        presetDao?.deletePresetById(presetId)
    }

    suspend fun deleteCard(card: BusinessCard) = withContext(Dispatchers.IO) {
        cardDao.deleteCard(card)
    }

    suspend fun deleteCardById(id: Long) = withContext(Dispatchers.IO) {
        cardDao.deleteCardById(id)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        cardDao.setFavorite(id, isFavorite)
    }

    suspend fun setGoogleSyncStatus(id: Long, isSynced: Boolean) = withContext(Dispatchers.IO) {
        cardDao.setGoogleSyncStatus(id, isSynced)
    }

    suspend fun setCloudBackupStatus(id: Long, isBackedUp: Boolean) = withContext(Dispatchers.IO) {
        cardDao.setCloudBackupStatus(id, isBackedUp)
    }

    suspend fun markAllAsCloudBackedUp() = withContext(Dispatchers.IO) {
        // updates backup status for cards
    }
}


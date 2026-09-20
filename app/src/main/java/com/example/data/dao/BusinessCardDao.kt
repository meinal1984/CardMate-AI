package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BusinessCard
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCardDao {

    @Query("SELECT * FROM business_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards WHERE id = :id")
    suspend fun getCardById(id: Long): BusinessCard?

    @Query("SELECT * FROM business_cards WHERE id = :id")
    fun getCardByIdFlow(id: Long): Flow<BusinessCard?>

    @Query("""
        SELECT * FROM business_cards 
        WHERE (:query = '' OR fullName LIKE '%' || :query || '%' OR company LIKE '%' || :query || '%' OR jobTitle LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        AND (:category = 'All' OR category = :category)
        ORDER BY createdAt DESC
    """)
    fun searchCards(query: String, category: String): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteCards(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards ORDER BY isFavorite DESC, updatedAt DESC LIMIT 25")
    fun getRecentAndFavoriteCardsSync(): List<BusinessCard>

    @Query("SELECT * FROM business_cards WHERE id = :id")
    fun getCardByIdSync(id: Long): BusinessCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: BusinessCard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<BusinessCard>)

    @Update
    suspend fun updateCard(card: BusinessCard)

    @Delete
    suspend fun deleteCard(card: BusinessCard)

    @Query("DELETE FROM business_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("UPDATE business_cards SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE business_cards SET isSyncedWithGoogleContacts = :isSynced, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setGoogleSyncStatus(id: Long, isSynced: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE business_cards SET isBackedUpToCloud = :isBackedUp, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCloudBackupStatus(id: Long, isBackedUp: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE business_cards SET
            customPrimaryBgColor = :primaryBg,
            customSecondaryBgColor = :secondaryBg,
            customAccentColor = :accent,
            customTextColor = :text,
            fontFamilyType = :fontFamily,
            layoutStyle = :layout,
            bgPattern = :pattern,
            cornerRadiusDp = :cornerRadius,
            borderStyle = :borderStyle,
            textAlignment = :alignment,
            showQrBadge = :showQr,
            showNfcBadge = :showNfc,
            showAvatar = :showAvatar,
            showCategoryBadge = :showCategory,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
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
        showCategory: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM business_cards")
    fun getCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM business_cards WHERE isSyncedWithGoogleContacts = 1")
    fun getSyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM business_cards WHERE isBackedUpToCloud = 1")
    fun getBackedUpCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM business_cards WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>
}

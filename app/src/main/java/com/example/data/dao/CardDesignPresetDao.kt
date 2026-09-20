package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CardDesignPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDesignPresetDao {

    @Query("SELECT * FROM card_design_presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<CardDesignPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CardDesignPreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<CardDesignPreset>)

    @Delete
    suspend fun deletePreset(preset: CardDesignPreset)

    @Query("DELETE FROM card_design_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)

    @Query("SELECT COUNT(*) FROM card_design_presets")
    suspend fun getPresetCount(): Int
}

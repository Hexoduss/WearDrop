package com.armadioai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeDao {

    // --- Capi ---

    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun observeItems(): Flow<List<ClothingItem>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItem(id: Long): Flow<ClothingItem?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Long): ClothingItem?

    @Insert
    suspend fun insertItem(item: ClothingItem): Long

    @Update
    suspend fun updateItem(item: ClothingItem)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    /** Se l'app viene chiusa durante un'analisi, il capo resterebbe "in analisi" per sempre. */
    @Query("UPDATE items SET status = 'error' WHERE status = 'analyzing'")
    suspend fun markInterruptedAnalyses()

    // --- Outfit salvati ---

    @Query("SELECT * FROM saved_outfits ORDER BY createdAt DESC")
    fun observeSaved(): Flow<List<SavedOutfit>>

    @Insert
    suspend fun insertSaved(outfit: SavedOutfit): Long

    @Delete
    suspend fun deleteSaved(outfit: SavedOutfit)

    // --- Storico "cosa ho indossato" ---

    @Query("SELECT * FROM wear_log ORDER BY wornAt DESC")
    fun observeWearLog(): Flow<List<WearLog>>

    @Query("SELECT * FROM wear_log WHERE wornAt >= :since")
    suspend fun wearSince(since: Long): List<WearLog>

    @Insert
    suspend fun insertWear(log: WearLog): Long

    @Delete
    suspend fun deleteWear(log: WearLog)
}

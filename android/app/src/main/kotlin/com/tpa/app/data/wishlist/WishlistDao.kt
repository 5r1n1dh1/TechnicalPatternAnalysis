package com.tpa.app.data.wishlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun all(): Flow<List<WishlistEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: WishlistEntry)

    @Query("DELETE FROM wishlist WHERE symbol = :symbol")
    suspend fun remove(symbol: String)

    @Query("SELECT * FROM wishlist WHERE symbol = :symbol LIMIT 1")
    suspend fun find(symbol: String): WishlistEntry?
}

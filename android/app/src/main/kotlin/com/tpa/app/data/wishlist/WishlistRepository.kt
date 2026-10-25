package com.tpa.app.data.wishlist

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

class WishlistRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        WishlistDb::class.java,
        "tpa.db"
    ).build()
    private val dao = db.dao()

    fun all(): Flow<List<WishlistEntry>> = dao.all()

    suspend fun add(symbol: String) {
        dao.add(WishlistEntry(symbol, System.currentTimeMillis()))
    }

    suspend fun remove(symbol: String) = dao.remove(symbol)

    suspend fun exists(symbol: String): Boolean = dao.find(symbol) != null
}

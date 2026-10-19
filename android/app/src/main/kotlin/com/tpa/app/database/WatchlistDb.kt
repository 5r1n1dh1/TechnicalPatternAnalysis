package com.tpa.app.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY symbol ASC")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchlistEntity)

    @Delete
    suspend fun delete(entity: WatchlistEntity)
}

@Database(entities = [WatchlistEntity::class], version = 1)
abstract class WatchlistDb : RoomDatabase() {
    abstract fun dao(): WatchlistDao
}

package com.tpa.app.data.wishlist

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WishlistEntry::class], version = 1)
abstract class WishlistDb : RoomDatabase() {
    abstract fun dao(): WishlistDao
}

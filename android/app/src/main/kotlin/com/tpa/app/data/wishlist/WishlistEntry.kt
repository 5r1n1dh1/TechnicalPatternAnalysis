package com.tpa.app.data.wishlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistEntry(
    @PrimaryKey val symbol: String,
    val addedAt: Long
)

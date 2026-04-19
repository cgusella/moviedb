package com.example.moviedb.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistMovie(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val director: String,
    val year: Int,
    val format: String = "DVD",
    val seriesName: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

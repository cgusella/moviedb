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
    val type: String = "Movie",
    val seriesName: String? = null,
    val posterUrl: String? = null,
    val durationMinutes: Int? = null,
    val genres: String? = null,
    val overview: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

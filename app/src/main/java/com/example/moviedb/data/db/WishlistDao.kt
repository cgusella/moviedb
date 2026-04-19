package com.example.moviedb.data.db

import androidx.room.*
import com.example.moviedb.data.model.WishlistMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun getAllWishlistMovies(): Flow<List<WishlistMovie>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWishlistMovie(movie: WishlistMovie): Long

    @Delete
    suspend fun deleteWishlistMovie(movie: WishlistMovie)
}

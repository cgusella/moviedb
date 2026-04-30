package com.example.moviedb.data.db

import androidx.room.*
import com.example.moviedb.data.model.WishlistMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun getAllWishlistMovies(): Flow<List<WishlistMovie>>

    @Query("""
        SELECT * FROM wishlist
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
        OR LOWER(director) LIKE '%' || LOWER(:query) || '%'
        ORDER BY addedAt DESC
    """)
    fun searchWishlistMovies(query: String): Flow<List<WishlistMovie>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE LOWER(title) = LOWER(:title) AND year = :year)")
    suspend fun wishlistExists(title: String, year: Int): Boolean

    @Query("SELECT * FROM wishlist WHERE id = :id")
    suspend fun getWishlistMovieById(id: Int): WishlistMovie?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWishlistMovie(movie: WishlistMovie): Long

    @Delete
    suspend fun deleteWishlistMovie(movie: WishlistMovie)

    @Query("DELETE FROM wishlist")
    suspend fun deleteAllWishlistMovies()
}

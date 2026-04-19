package com.example.moviedb.data.db

import androidx.room.*
import com.example.moviedb.data.model.Movie
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY LOWER(title) ASC")
    fun getAllMovies(): Flow<List<Movie>>

    @Query("""
        SELECT * FROM movies
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
        OR LOWER(director) LIKE '%' || LOWER(:query) || '%'
        ORDER BY LOWER(title) ASC
    """)
    fun searchMovies(query: String): Flow<List<Movie>>

    @Query("SELECT EXISTS(SELECT 1 FROM movies WHERE LOWER(title) = LOWER(:title) AND year = :year)")
    suspend fun movieExists(title: String, year: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovie(movie: Movie): Long

    @Delete
    suspend fun deleteMovie(movie: Movie)
}

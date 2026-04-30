package com.example.moviedb.data.db

import androidx.room.*
import com.example.moviedb.data.model.Movie
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY LOWER(title) ASC")
    fun getAllMovies(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY LOWER(title) ASC")
    fun getByTitleAsc(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY LOWER(title) DESC")
    fun getByTitleDesc(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY LOWER(director) ASC, LOWER(title) ASC")
    fun getByDirectorAsc(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY LOWER(director) DESC, LOWER(title) ASC")
    fun getByDirectorDesc(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY year ASC, LOWER(title) ASC")
    fun getByYearAsc(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY year DESC, LOWER(title) ASC")
    fun getByYearDesc(): Flow<List<Movie>>

    @Query("SELECT * FROM movies ORDER BY addedAt DESC")
    fun getByAddedAtDesc(): Flow<List<Movie>>

    @Query("""
        SELECT * FROM movies
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
        OR LOWER(director) LIKE '%' || LOWER(:query) || '%'
        ORDER BY LOWER(title) ASC
    """)
    fun searchMovies(query: String): Flow<List<Movie>>

    @Query("SELECT EXISTS(SELECT 1 FROM movies WHERE LOWER(title) = LOWER(:title) AND year = :year)")
    suspend fun movieExists(title: String, year: Int): Boolean

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): Movie?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovie(movie: Movie): Long

    @Update
    suspend fun updateMovie(movie: Movie)

    @Delete
    suspend fun deleteMovie(movie: Movie)

    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()
}

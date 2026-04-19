package com.example.moviedb.data.repository

import com.example.moviedb.data.db.MovieDao
import com.example.moviedb.data.db.WishlistDao
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.model.WishlistMovie
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val movieDao: MovieDao,
    private val wishlistDao: WishlistDao
) {
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()
    val allWishlistMovies: Flow<List<WishlistMovie>> = wishlistDao.getAllWishlistMovies()

    fun searchMovies(query: String): Flow<List<Movie>> = movieDao.searchMovies(query)

    suspend fun movieExists(title: String, year: Int): Boolean =
        movieDao.movieExists(title, year)

    suspend fun addMovie(movie: Movie): Long = movieDao.insertMovie(movie)

    suspend fun deleteMovie(movie: Movie) = movieDao.deleteMovie(movie)

    suspend fun addToWishlist(movie: WishlistMovie): Long =
        wishlistDao.insertWishlistMovie(movie)

    suspend fun removeFromWishlist(movie: WishlistMovie) =
        wishlistDao.deleteWishlistMovie(movie)

    suspend fun promoteToCollection(wishlistMovie: WishlistMovie) {
        wishlistDao.deleteWishlistMovie(wishlistMovie)
        movieDao.insertMovie(
            Movie(
                title = wishlistMovie.title,
                director = wishlistMovie.director,
                year = wishlistMovie.year,
                format = wishlistMovie.format,
                seriesName = wishlistMovie.seriesName
            )
        )
    }
}

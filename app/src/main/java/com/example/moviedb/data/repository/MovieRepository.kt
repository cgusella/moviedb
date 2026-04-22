package com.example.moviedb.data.repository

import com.example.moviedb.data.db.MovieDao
import com.example.moviedb.data.db.WishlistDao
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.model.WishlistMovie
import com.example.moviedb.ui.screens.collection.SortDirection
import com.example.moviedb.ui.screens.collection.SortField
import com.example.moviedb.ui.screens.collection.SortOption
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val movieDao: MovieDao,
    private val wishlistDao: WishlistDao
) {
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()
    val allWishlistMovies: Flow<List<WishlistMovie>> = wishlistDao.getAllWishlistMovies()

    fun getMoviesSorted(sortOption: SortOption): Flow<List<Movie>> = when (sortOption.field) {
        SortField.TITLE -> if (sortOption.direction == SortDirection.ASC) movieDao.getByTitleAsc() else movieDao.getByTitleDesc()
        SortField.DIRECTOR -> if (sortOption.direction == SortDirection.ASC) movieDao.getByDirectorAsc() else movieDao.getByDirectorDesc()
        SortField.YEAR -> if (sortOption.direction == SortDirection.ASC) movieDao.getByYearAsc() else movieDao.getByYearDesc()
    }

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
                type = wishlistMovie.type,
                seriesName = wishlistMovie.seriesName,
                posterUrl = wishlistMovie.posterUrl,
                durationMinutes = wishlistMovie.durationMinutes,
                genres = wishlistMovie.genres
            )
        )
    }
}

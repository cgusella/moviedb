package com.example.moviedb.data.sync

import com.example.moviedb.data.remote.MovieLookupResult
import com.example.moviedb.data.remote.MovieLookupService
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.delay

class BackfillService(
    private val repository: MovieRepository,
    private val lookupService: MovieLookupService,
    private val language: String
) {
    suspend fun run(): Int {
        var count = 0

        for (movie in repository.getMoviesWithoutCast()) {
            val r = fetchBestMatch(movie.title, movie.year, movie.type) ?: continue
            repository.updateMovie(
                movie.copy(
                    director = movie.director.ifBlank { r.director },
                    posterUrl = movie.posterUrl ?: r.posterUrl,
                    durationMinutes = movie.durationMinutes ?: r.durationMinutes,
                    genres = movie.genres ?: r.genres.joinToString(", ").ifBlank { null },
                    overview = movie.overview ?: r.overview,
                    cast = r.cast.joinToString(", ").ifBlank { null },
                    trailerKey = movie.trailerKey ?: r.trailerKey
                )
            )
            count++
            delay(300)
        }

        for (item in repository.getWishlistWithoutCast()) {
            val r = fetchBestMatch(item.title, item.year, item.type) ?: continue
            repository.updateWishlistMovie(
                item.copy(
                    director = item.director.ifBlank { r.director },
                    posterUrl = item.posterUrl ?: r.posterUrl,
                    durationMinutes = item.durationMinutes ?: r.durationMinutes,
                    genres = item.genres ?: r.genres.joinToString(", ").ifBlank { null },
                    overview = item.overview ?: r.overview,
                    cast = r.cast.joinToString(", ").ifBlank { null },
                    trailerKey = item.trailerKey ?: r.trailerKey
                )
            )
            count++
            delay(300)
        }

        return count
    }

    private suspend fun fetchBestMatch(title: String, year: Int, type: String): MovieLookupResult? {
        val results = if (type == "TV Series")
            lookupService.searchTvByTitle(title, language)
        else
            lookupService.searchByTitle(title, language)
        val match = results.firstOrNull { it.year == year.toString() }
            ?: results.firstOrNull()
            ?: return null
        return if (type == "TV Series")
            lookupService.fetchTvById(match.id, language)
        else
            lookupService.fetchMovieById(match.id, language)
    }
}

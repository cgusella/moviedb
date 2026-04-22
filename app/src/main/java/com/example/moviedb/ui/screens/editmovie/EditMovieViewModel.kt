package com.example.moviedb.ui.screens.editmovie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.preferences.SettingsRepository
import com.example.moviedb.data.remote.MovieLookupService
import com.example.moviedb.data.remote.TmdbSearchResult
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class TitleSearchState {
    object Idle : TitleSearchState()
    object Loading : TitleSearchState()
    data class Results(val items: List<TmdbSearchResult>) : TitleSearchState()
    data class Error(val message: String) : TitleSearchState()
}

sealed class EditMovieUiState {
    object Loading : EditMovieUiState()
    object Idle : EditMovieUiState()
    object Saved : EditMovieUiState()
    object NotFound : EditMovieUiState()
    data class ValidationError(
        val titleError: String?,
        val directorError: String?,
        val yearError: String?,
        val seriesNameError: String?
    ) : EditMovieUiState()
}

class EditMovieViewModel(
    application: Application,
    private val repository: MovieRepository,
    private val movieId: Int
) : AndroidViewModel(application) {

    private val lookupService = MovieLookupService()

    private val languageCode: StateFlow<String> = SettingsRepository
        .getLanguageCode(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "it-IT")

    private var originalMovie: Movie? = null

    private val _uiState = MutableStateFlow<EditMovieUiState>(EditMovieUiState.Loading)
    val uiState: StateFlow<EditMovieUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _director = MutableStateFlow("")
    val director: StateFlow<String> = _director.asStateFlow()

    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _format = MutableStateFlow("DVD")
    val format: StateFlow<String> = _format.asStateFlow()

    private val _belongsToSeries = MutableStateFlow(false)
    val belongsToSeries: StateFlow<Boolean> = _belongsToSeries.asStateFlow()

    private val _seriesName = MutableStateFlow("")
    val seriesName: StateFlow<String> = _seriesName.asStateFlow()

    private val _posterUrl = MutableStateFlow<String?>(null)
    private val _durationMinutes = MutableStateFlow<Int?>(null)
    private val _genres = MutableStateFlow<String?>(null)
    private val _type = MutableStateFlow("Movie")

    private val _titleSearchQuery = MutableStateFlow("")
    val titleSearchQuery: StateFlow<String> = _titleSearchQuery.asStateFlow()

    private val _titleSearchState = MutableStateFlow<TitleSearchState>(TitleSearchState.Idle)
    val titleSearchState: StateFlow<TitleSearchState> = _titleSearchState.asStateFlow()

    private val _searchType = MutableStateFlow(com.example.moviedb.ui.screens.addmovie.SearchType.MOVIE)
    val searchType: StateFlow<com.example.moviedb.ui.screens.addmovie.SearchType> = _searchType.asStateFlow()

    init {
        viewModelScope.launch {
            val movie = repository.getMovieById(movieId)
            if (movie == null) {
                _uiState.value = EditMovieUiState.NotFound
                return@launch
            }
            originalMovie = movie
            _title.value = movie.title
            _director.value = movie.director
            _year.value = movie.year.toString()
            _format.value = movie.format
            _belongsToSeries.value = movie.seriesName != null
            _seriesName.value = movie.seriesName ?: ""
            _posterUrl.value = movie.posterUrl
            _durationMinutes.value = movie.durationMinutes
            _genres.value = movie.genres
            _type.value = movie.type
            _uiState.value = EditMovieUiState.Idle
        }
    }

    fun onTitleChange(v: String) { _title.value = v; resetIfError() }
    fun onDirectorChange(v: String) { _director.value = v; resetIfError() }
    fun onYearChange(v: String) { if (v.length <= 4 && v.all { it.isDigit() }) { _year.value = v; resetIfError() } }
    fun onFormatChange(v: String) { _format.value = v }
    fun onBelongsToSeriesChange(v: Boolean) { _belongsToSeries.value = v; if (!v) _seriesName.value = "" }
    fun onSeriesNameChange(v: String) { _seriesName.value = v; resetIfError() }
    fun onTitleSearchQueryChange(v: String) { _titleSearchQuery.value = v }
    fun onSearchTypeChange(type: com.example.moviedb.ui.screens.addmovie.SearchType) { _searchType.value = type }

    private fun resetIfError() {
        if (_uiState.value is EditMovieUiState.ValidationError) _uiState.value = EditMovieUiState.Idle
    }

    fun onTitleSearch() {
        val query = _titleSearchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _titleSearchState.value = TitleSearchState.Loading
            val results = if (_searchType.value == com.example.moviedb.ui.screens.addmovie.SearchType.MOVIE)
                lookupService.searchByTitle(query, languageCode.value)
            else
                lookupService.searchTvByTitle(query, languageCode.value)
            _titleSearchState.value = if (results.isEmpty())
                TitleSearchState.Error("No results found for \"$query\"")
            else
                TitleSearchState.Results(results)
        }
    }

    fun onTitleSearchResultSelected(tmdbId: Int, type: String) {
        viewModelScope.launch {
            _titleSearchState.value = TitleSearchState.Loading
            val result = if (type == "TV Series")
                lookupService.fetchTvById(tmdbId, languageCode.value)
            else
                lookupService.fetchMovieById(tmdbId, languageCode.value)
            _titleSearchState.value = TitleSearchState.Idle
            if (result != null) {
                _title.value = result.title
                _director.value = result.director
                _year.value = result.year
                _posterUrl.value = result.posterUrl
                _durationMinutes.value = result.durationMinutes
                _genres.value = result.genres.joinToString(", ").ifBlank { null }
                _type.value = result.type
            }
        }
    }

    fun dismissTitleSearch() { _titleSearchState.value = TitleSearchState.Idle }

    fun onSaveClick() {
        val titleErr = if (_title.value.isBlank()) "Title is required" else null
        val directorErr = if (_director.value.isBlank()) "Director / Creator is required" else null
        val yearInt = _year.value.toIntOrNull()
        val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 5
        val yearErr = when {
            _year.value.isBlank() -> "Year is required"
            yearInt == null -> "Enter a valid year"
            yearInt < 1888 || yearInt > maxYear -> "Year must be between 1888 and $maxYear"
            else -> null
        }
        val seriesNameErr = if (_belongsToSeries.value && _seriesName.value.isBlank())
            "Series name is required" else null

        if (titleErr != null || directorErr != null || yearErr != null || seriesNameErr != null) {
            _uiState.value = EditMovieUiState.ValidationError(titleErr, directorErr, yearErr, seriesNameErr)
            return
        }

        val original = originalMovie ?: return
        viewModelScope.launch {
            repository.updateMovie(
                original.copy(
                    title = _title.value.trim(),
                    director = _director.value.trim(),
                    year = yearInt!!,
                    format = _format.value,
                    type = _type.value,
                    seriesName = if (_belongsToSeries.value) _seriesName.value.trim().ifBlank { null } else null,
                    posterUrl = _posterUrl.value,
                    durationMinutes = _durationMinutes.value,
                    genres = _genres.value
                )
            )
            _uiState.value = EditMovieUiState.Saved
        }
    }

    companion object {
        fun factory(repository: MovieRepository, movieId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                EditMovieViewModel(app, repository, movieId)
            }
        }
    }
}

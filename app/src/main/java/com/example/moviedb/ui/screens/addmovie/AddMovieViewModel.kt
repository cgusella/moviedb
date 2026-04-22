package com.example.moviedb.ui.screens.addmovie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.model.WishlistMovie
import com.example.moviedb.data.preferences.SettingsRepository
import com.example.moviedb.data.remote.MovieLookupService
import com.example.moviedb.data.remote.TmdbSearchResult
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SearchType { MOVIE, TV }

sealed class TitleSearchState {
    object Idle : TitleSearchState()
    object Loading : TitleSearchState()
    data class Results(val items: List<TmdbSearchResult>) : TitleSearchState()
    data class Error(val message: String) : TitleSearchState()
}

sealed class AddMovieUiState {
    object Idle : AddMovieUiState()
    object Loading : AddMovieUiState()
    data class DuplicateWarning(val destination: Destination) : AddMovieUiState()
    data class Success(val destination: Destination) : AddMovieUiState()
    data class ValidationError(
        val titleError: String?,
        val directorError: String?,
        val yearError: String?,
        val seriesNameError: String?
    ) : AddMovieUiState()
}

enum class Destination { COLLECTION, WISHLIST }

class AddMovieViewModel(
    application: Application,
    private val repository: MovieRepository
) : AndroidViewModel(application) {

    private val lookupService = MovieLookupService()

    private val languageCode: StateFlow<String> = SettingsRepository
        .getLanguageCode(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "it-IT")

    private val _searchType = MutableStateFlow(SearchType.MOVIE)
    val searchType: StateFlow<SearchType> = _searchType.asStateFlow()

    fun onSearchTypeChange(type: SearchType) { _searchType.value = type }

    private val _titleSearchQuery = MutableStateFlow("")
    val titleSearchQuery: StateFlow<String> = _titleSearchQuery.asStateFlow()

    private val _titleSearchState = MutableStateFlow<TitleSearchState>(TitleSearchState.Idle)
    val titleSearchState: StateFlow<TitleSearchState> = _titleSearchState.asStateFlow()

    fun onTitleSearchQueryChange(v: String) { _titleSearchQuery.value = v }

    fun onTitleSearch() {
        val query = _titleSearchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _titleSearchState.value = TitleSearchState.Loading
            val results = if (_searchType.value == SearchType.MOVIE)
                lookupService.searchByTitle(query, languageCode.value)
            else
                lookupService.searchTvByTitle(query, languageCode.value)
            _titleSearchState.value = if (results.isEmpty())
                TitleSearchState.Error("No results found for \"$query\"")
            else
                TitleSearchState.Results(results)
        }
    }

    private val _posterUrl = MutableStateFlow<String?>(null)
    private val _durationMinutes = MutableStateFlow<Int?>(null)
    private val _type = MutableStateFlow("Movie")

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
                _type.value = result.type
            }
        }
    }

    fun dismissTitleSearch() { _titleSearchState.value = TitleSearchState.Idle }

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

    private val _uiState = MutableStateFlow<AddMovieUiState>(AddMovieUiState.Idle)
    val uiState: StateFlow<AddMovieUiState> = _uiState.asStateFlow()

    fun onTitleChange(v: String) { _title.value = v; resetIfError() }
    fun onDirectorChange(v: String) { _director.value = v; resetIfError() }
    fun onYearChange(v: String) { if (v.length <= 4 && v.all { it.isDigit() }) { _year.value = v; resetIfError() } }
    fun onFormatChange(v: String) { _format.value = v }
    fun onBelongsToSeriesChange(v: Boolean) { _belongsToSeries.value = v; if (!v) _seriesName.value = "" }
    fun onSeriesNameChange(v: String) { _seriesName.value = v; resetIfError() }

    private fun resetIfError() {
        if (_uiState.value is AddMovieUiState.ValidationError) _uiState.value = AddMovieUiState.Idle
    }

    fun onSaveClick(destination: Destination) {
        val titleErr = if (_title.value.isBlank()) "Title is required" else null
        val directorErr = if (_director.value.isBlank()) "Director / Creator is required" else null

        val yearInt = _year.value.toIntOrNull()
        val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 5
        val yearErr = if (destination == Destination.COLLECTION) when {
            _year.value.isBlank() -> "Year is required"
            yearInt == null -> "Enter a valid year"
            yearInt < 1888 || yearInt > maxYear -> "Year must be between 1888 and $maxYear"
            else -> null
        } else null

        val seriesNameErr = if (destination == Destination.COLLECTION
            && _belongsToSeries.value
            && _seriesName.value.isBlank()
        ) "Series name is required" else null

        if (titleErr != null || directorErr != null || yearErr != null || seriesNameErr != null) {
            _uiState.value = AddMovieUiState.ValidationError(titleErr, directorErr, yearErr, seriesNameErr)
            return
        }

        viewModelScope.launch {
            _uiState.value = AddMovieUiState.Loading
            val exists = destination == Destination.COLLECTION &&
                repository.movieExists(_title.value.trim(), yearInt!!)
            if (exists) {
                _uiState.value = AddMovieUiState.DuplicateWarning(destination)
            } else {
                save(destination, yearInt)
            }
        }
    }

    fun onConfirmDuplicate() {
        val dest = (_uiState.value as? AddMovieUiState.DuplicateWarning)?.destination ?: return
        viewModelScope.launch { save(dest, _year.value.toIntOrNull()) }
    }

    private suspend fun save(destination: Destination, year: Int?) {
        val sName = if (_belongsToSeries.value) _seriesName.value.trim().ifBlank { null } else null
        when (destination) {
            Destination.COLLECTION -> repository.addMovie(
                Movie(
                    title = _title.value.trim(),
                    director = _director.value.trim(),
                    year = year ?: 0,
                    format = _format.value,
                    type = _type.value,
                    seriesName = sName,
                    posterUrl = _posterUrl.value,
                    durationMinutes = _durationMinutes.value
                )
            )
            Destination.WISHLIST -> repository.addToWishlist(
                WishlistMovie(
                    title = _title.value.trim(),
                    director = _director.value.trim(),
                    year = year ?: 0,
                    format = _format.value,
                    type = _type.value,
                    seriesName = sName,
                    posterUrl = _posterUrl.value,
                    durationMinutes = _durationMinutes.value
                )
            )
        }
        _uiState.value = AddMovieUiState.Success(destination)
    }

    fun resetForm() {
        _title.value = ""; _director.value = ""; _year.value = ""; _format.value = "DVD"
        _belongsToSeries.value = false; _seriesName.value = ""
        _uiState.value = AddMovieUiState.Idle
        _titleSearchQuery.value = ""
        _posterUrl.value = null
        _durationMinutes.value = null
        _type.value = "Movie"
    }

    companion object {
        fun factory(repository: MovieRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                AddMovieViewModel(app, repository)
            }
        }
    }
}

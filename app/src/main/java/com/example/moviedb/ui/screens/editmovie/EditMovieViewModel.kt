package com.example.moviedb.ui.screens.editmovie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private val _posterUrl = MutableStateFlow("")
    val posterUrl: StateFlow<String> = _posterUrl.asStateFlow()

    private val _durationText = MutableStateFlow("")
    val durationText: StateFlow<String> = _durationText.asStateFlow()

    private val _genres = MutableStateFlow("")
    val genres: StateFlow<String> = _genres.asStateFlow()

    private val _type = MutableStateFlow("Movie")
    val type: StateFlow<String> = _type.asStateFlow()

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
            _posterUrl.value = movie.posterUrl ?: ""
            _durationText.value = movie.durationMinutes?.toString() ?: ""
            _genres.value = movie.genres ?: ""
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
    fun onTypeChange(v: String) { _type.value = v }
    fun onPosterUrlChange(v: String) { _posterUrl.value = v }
    fun onDurationChange(v: String) { if (v.all { it.isDigit() }) _durationText.value = v }
    fun onGenresChange(v: String) { _genres.value = v }
    private fun resetIfError() {
        if (_uiState.value is EditMovieUiState.ValidationError) _uiState.value = EditMovieUiState.Idle
    }

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
                    posterUrl = _posterUrl.value.ifBlank { null },
                    durationMinutes = _durationText.value.toIntOrNull(),
                    genres = _genres.value.ifBlank { null }
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

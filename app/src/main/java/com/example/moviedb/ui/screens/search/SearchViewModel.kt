package com.example.moviedb.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    val selectedGenres: StateFlow<Set<String>> = _selectedGenres.asStateFlow()

    val availableGenres: StateFlow<List<String>> = repository.allMovies
        .map { movies ->
            movies.flatMap { it.genres?.split(", ").orEmpty() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    val results: StateFlow<List<Movie>> = combine(
        repository.allMovies,
        _query.debounce(300),
        _selectedGenres
    ) { all, q, genres ->
        if (q.isBlank() && genres.isEmpty()) emptyList()
        else all
            .filter { movie ->
                q.isBlank() ||
                    movie.title.contains(q, ignoreCase = true) ||
                    movie.director.contains(q, ignoreCase = true)
            }
            .filter { movie ->
                genres.isEmpty() || movie.genres?.split(", ")?.let { genres.all { g -> g in it } } == true
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isInCollection: StateFlow<Boolean> = results
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.isNotBlank()) _selectedGenres.value = emptySet()
    }

    fun onGenreSelected(genre: String) {
        _selectedGenres.value = _selectedGenres.value.toMutableSet().apply {
            if (!add(genre)) remove(genre)
        }
    }

    companion object {
        fun factory(repository: MovieRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}

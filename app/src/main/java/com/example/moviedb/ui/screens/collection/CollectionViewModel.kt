package com.example.moviedb.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CollectionViewModel(private val repository: MovieRepository) : ViewModel() {

    val filteredMovies: StateFlow<List<Movie>> = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch { repository.deleteMovie(movie) }
    }

    companion object {
        fun factory(repository: MovieRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CollectionViewModel(repository) }
        }
    }
}

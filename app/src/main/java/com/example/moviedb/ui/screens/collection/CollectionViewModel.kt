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

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds: StateFlow<Set<Int>> = _selectedIds.asStateFlow()

    fun toggleSelection(id: Int) {
        _isSelectionMode.value = true
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = filteredMovies.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch { repository.deleteMovie(movie) }
    }

    fun deleteSelected() {
        val toDelete = filteredMovies.value.filter { it.id in _selectedIds.value }
        viewModelScope.launch {
            toDelete.forEach { repository.deleteMovie(it) }
            _isSelectionMode.value = false
            _selectedIds.value = emptySet()
        }
    }

    companion object {
        fun factory(repository: MovieRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CollectionViewModel(repository) }
        }
    }
}

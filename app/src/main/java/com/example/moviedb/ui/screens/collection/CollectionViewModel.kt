package com.example.moviedb.ui.screens.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.preferences.SettingsRepository
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CollectionViewModel(
    app: Application,
    private val repository: MovieRepository
) : AndroidViewModel(app) {

    private val _sortOption = MutableStateFlow(SortOption(SortField.ADDED_AT))
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    init {
        // Keep _sortOption in sync with the DataStore preference (set by Settings screen).
        // Only update the field; preserve direction if the field didn't change.
        viewModelScope.launch {
            SettingsRepository.getSortOwned(app).collect { stored ->
                val incoming = sortOptionFromString(stored)
                if (_sortOption.value.field != incoming.field) {
                    _sortOption.value = incoming
                }
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredMovies: StateFlow<List<Movie>> = combine(_sortOption, _searchQuery) { sort, query ->
        Pair(sort, query)
    }.flatMapLatest { (sort, query) ->
        if (query.isBlank()) repository.getMoviesSorted(sort) else repository.searchMovies(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _viewMode = MutableStateFlow("list")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    init {
        // Initialise from DataStore and react to Settings changes, but local
        // toggle (setViewMode) does not write back — Settings is the authority.
        viewModelScope.launch {
            SettingsRepository.getCollectionView(app).collect { _viewMode.value = it }
        }
    }

    fun setViewMode(mode: String) { _viewMode.value = mode }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun toggleSort(field: SortField) {
        _sortOption.update { current ->
            if (current.field == field)
                current.copy(direction = if (current.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC)
            else
                SortOption(field)
        }
    }

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

    fun selectAll() { _selectedIds.value = filteredMovies.value.map { it.id }.toSet() }

    fun deselectAll() { _selectedIds.value = emptySet() }

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
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                CollectionViewModel(app, repository)
            }
        }
    }
}

private fun sortOptionFromString(stored: String) = SortOption(
    field = when (stored) {
        "title"    -> SortField.TITLE
        "director" -> SortField.DIRECTOR
        "year"     -> SortField.YEAR
        else       -> SortField.ADDED_AT
    }
)

private fun sortFieldToString(field: SortField) = when (field) {
    SortField.TITLE    -> "title"
    SortField.DIRECTOR -> "director"
    SortField.YEAR     -> "year"
    SortField.ADDED_AT -> "recently_added"
}

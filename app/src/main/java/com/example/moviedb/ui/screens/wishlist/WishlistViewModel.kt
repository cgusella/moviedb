package com.example.moviedb.ui.screens.wishlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.WishlistMovie
import com.example.moviedb.data.preferences.SettingsRepository
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WishlistViewModel(
    app: Application,
    private val repository: MovieRepository
) : AndroidViewModel(app) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val wishlistMovies: StateFlow<List<WishlistMovie>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.allWishlistMovies else repository.searchWishlistMovies(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _viewMode = MutableStateFlow("list")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsRepository.getWishlistView(app).collect { _viewMode.value = it }
        }
    }

    fun setViewMode(mode: String) { _viewMode.value = mode }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    fun removeFromWishlist(movie: WishlistMovie) {
        viewModelScope.launch { repository.removeFromWishlist(movie) }
    }

    fun promoteToCollection(movie: WishlistMovie) {
        viewModelScope.launch {
            repository.promoteToCollection(movie)
            _events.send("\"${movie.title}\" moved to your collection!")
        }
    }

    companion object {
        fun factory(repository: MovieRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                WishlistViewModel(app, repository)
            }
        }
    }
}

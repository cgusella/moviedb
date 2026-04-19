package com.example.moviedb.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.model.WishlistMovie
import com.example.moviedb.data.repository.MovieRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WishlistViewModel(private val repository: MovieRepository) : ViewModel() {

    val wishlistMovies: StateFlow<List<WishlistMovie>> = repository.allWishlistMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            initializer { WishlistViewModel(repository) }
        }
    }
}

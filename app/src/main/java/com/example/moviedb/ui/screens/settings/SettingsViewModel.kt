package com.example.moviedb.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviedb.data.preferences.SettingsRepository
import com.example.moviedb.data.remote.MovieLookupService
import com.example.moviedb.data.repository.MovieRepository
import com.example.moviedb.data.sync.BackfillService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackfillState {
    object Idle : BackfillState()
    object Running : BackfillState()
    data class Done(val updated: Int) : BackfillState()
}

class SettingsViewModel(
    app: Application,
    private val repository: MovieRepository
) : AndroidViewModel(app) {

    val languageCode: StateFlow<String> = SettingsRepository
        .getLanguageCode(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "it-IT")

    val appearance: StateFlow<String> = SettingsRepository
        .getAppearance(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val sortOwned: StateFlow<String> = SettingsRepository
        .getSortOwned(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "title")

    val collectionView: StateFlow<String> = SettingsRepository
        .getCollectionView(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "grid")

    fun setLanguage(code: String) {
        viewModelScope.launch { SettingsRepository.setLanguageCode(getApplication(), code) }
    }

    fun setAppearance(value: String) {
        viewModelScope.launch { SettingsRepository.setAppearance(getApplication(), value) }
    }

    fun setSortOwned(value: String) {
        viewModelScope.launch { SettingsRepository.setSortOwned(getApplication(), value) }
    }

    fun setDefaultView(value: String) {
        viewModelScope.launch {
            SettingsRepository.setCollectionView(getApplication(), value)
            SettingsRepository.setWishlistView(getApplication(), value)
        }
    }

    private val _backfillState = MutableStateFlow<BackfillState>(BackfillState.Idle)
    val backfillState: StateFlow<BackfillState> = _backfillState.asStateFlow()

    fun enrichFromTmdb() {
        if (_backfillState.value is BackfillState.Running) return
        viewModelScope.launch {
            _backfillState.value = BackfillState.Running
            val lang = languageCode.first()
            val count = BackfillService(repository, MovieLookupService(), lang).run()
            _backfillState.value = BackfillState.Done(count)
        }
    }

    fun resetBackfillState() { _backfillState.value = BackfillState.Idle }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            onDone()
        }
    }

    companion object {
        fun factory(repository: MovieRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                SettingsViewModel(app, repository)
            }
        }
    }
}

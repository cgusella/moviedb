package com.example.moviedb.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsRepository {
    private val KEY_LANGUAGE = stringPreferencesKey("language_code")

    fun getLanguageCode(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_LANGUAGE] ?: "it-IT" }

    suspend fun setLanguageCode(context: Context, code: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = code }
    }
}

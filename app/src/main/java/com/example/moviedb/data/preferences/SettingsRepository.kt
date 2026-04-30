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
    private val KEY_COLLECTION_VIEW = stringPreferencesKey("collection_view")
    private val KEY_WISHLIST_VIEW = stringPreferencesKey("wishlist_view")
    private val KEY_APPEARANCE = stringPreferencesKey("appearance")
    private val KEY_SORT_OWNED = stringPreferencesKey("sort_owned")

    fun getLanguageCode(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_LANGUAGE] ?: "it-IT" }

    suspend fun setLanguageCode(context: Context, code: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    fun getCollectionView(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_COLLECTION_VIEW] ?: "grid" }

    suspend fun setCollectionView(context: Context, view: String) {
        context.dataStore.edit { it[KEY_COLLECTION_VIEW] = view }
    }

    fun getWishlistView(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_WISHLIST_VIEW] ?: "list" }

    suspend fun setWishlistView(context: Context, view: String) {
        context.dataStore.edit { it[KEY_WISHLIST_VIEW] = view }
    }

    fun getAppearance(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_APPEARANCE] ?: "system" }

    suspend fun setAppearance(context: Context, value: String) {
        context.dataStore.edit { it[KEY_APPEARANCE] = value }
    }

    fun getSortOwned(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_SORT_OWNED] ?: "title" }

    suspend fun setSortOwned(context: Context, value: String) {
        context.dataStore.edit { it[KEY_SORT_OWNED] = value }
    }
}

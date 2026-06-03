package com.andreilima.capychat.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val DARK_THEME           = booleanPreferencesKey("dark_theme")
        val REDUCE_ANIMATIONS    = booleanPreferencesKey("reduce_animations")
        val VIBRATION_ENABLED    = booleanPreferencesKey("vibration_enabled")
        val SOUNDS_ENABLED       = booleanPreferencesKey("sounds_enabled")
        val EXPERIMENTAL_ENABLED = booleanPreferencesKey("experimental_features")
        // Privacidade
        val SHOW_ONLINE_STATUS   = booleanPreferencesKey("show_online_status")
        val SHOW_READ_RECEIPTS   = booleanPreferencesKey("show_read_receipts")
        val SHOW_LAST_SEEN       = booleanPreferencesKey("show_last_seen")
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map {
        it[DARK_THEME] ?: false
    }
    val reduceAnimations: Flow<Boolean> = context.dataStore.data.map {
        it[REDUCE_ANIMATIONS] ?: false
    }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[VIBRATION_ENABLED] ?: true
    }
    val soundsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[SOUNDS_ENABLED] ?: true
    }
    val experimentalEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[EXPERIMENTAL_ENABLED] ?: false
    }
    val showOnlineStatus: Flow<Boolean> = context.dataStore.data.map {
        it[SHOW_ONLINE_STATUS] ?: true
    }
    val showReadReceipts: Flow<Boolean> = context.dataStore.data.map {
        it[SHOW_READ_RECEIPTS] ?: true
    }
    val showLastSeen: Flow<Boolean> = context.dataStore.data.map {
        it[SHOW_LAST_SEEN] ?: true
    }

    suspend fun setDarkTheme(value: Boolean) =
        context.dataStore.edit { it[DARK_THEME] = value }

    suspend fun setReduceAnimations(value: Boolean) =
        context.dataStore.edit { it[REDUCE_ANIMATIONS] = value }

    suspend fun setVibrationEnabled(value: Boolean) =
        context.dataStore.edit { it[VIBRATION_ENABLED] = value }

    suspend fun setSoundsEnabled(value: Boolean) =
        context.dataStore.edit { it[SOUNDS_ENABLED] = value }

    suspend fun setExperimentalEnabled(value: Boolean) =
        context.dataStore.edit { it[EXPERIMENTAL_ENABLED] = value }

    suspend fun setShowOnlineStatus(value: Boolean) =
        context.dataStore.edit { it[SHOW_ONLINE_STATUS] = value }

    suspend fun setShowReadReceipts(value: Boolean) =
        context.dataStore.edit { it[SHOW_READ_RECEIPTS] = value }

    suspend fun setShowLastSeen(value: Boolean) =
        context.dataStore.edit { it[SHOW_LAST_SEEN] = value }
}
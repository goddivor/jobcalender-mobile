package tg.goddivor.jobcalender.data.remote

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncStore by preferencesDataStore(name = "sync")

/**
 * What the app learned from `/api/config`, plus when it last synced. The token is fetched, never
 * typed, and never shown.
 */
@Singleton
class SyncSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val state: Flow<SyncState> = context.syncStore.data.map { it.toState() }

    suspend fun store(apiUrl: String, token: String) {
        context.syncStore.edit {
            it[API_URL] = apiUrl
            it[TOKEN] = token
        }
    }

    suspend fun markSynced(at: Instant) {
        context.syncStore.edit { it[LAST_SYNC] = at.toString() }
    }

    suspend fun setSyncOnLaunch(enabled: Boolean) {
        context.syncStore.edit { it[SYNC_ON_LAUNCH] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.syncStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.syncStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    private fun Preferences.toState() = SyncState(
        apiUrl = this[API_URL],
        token = this[TOKEN],
        lastSyncAt = this[LAST_SYNC]?.let(Instant::parse),
        syncOnLaunch = this[SYNC_ON_LAUNCH] ?: true,
        themeMode = this[THEME_MODE] ?: FOLLOW_SYSTEM,
        dynamicColor = this[DYNAMIC_COLOR] ?: false,
    )

    private companion object {
        val API_URL = stringPreferencesKey("api_url")
        val TOKEN = stringPreferencesKey("token")
        val LAST_SYNC = stringPreferencesKey("last_sync")
        val SYNC_ON_LAUNCH = booleanPreferencesKey("sync_on_launch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        const val FOLLOW_SYSTEM = "FOLLOW_SYSTEM"
    }
}

data class SyncState(
    val apiUrl: String? = null,
    val token: String? = null,
    val lastSyncAt: Instant? = null,
    val syncOnLaunch: Boolean = true,
    val themeMode: String = "FOLLOW_SYSTEM",
    val dynamicColor: Boolean = false,
) {
    val isConfigured: Boolean get() = apiUrl != null && token != null
}

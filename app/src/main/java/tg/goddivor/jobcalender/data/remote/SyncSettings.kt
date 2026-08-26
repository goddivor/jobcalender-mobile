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
 * What the app learned from `/api/config`, plus the preferences that outlive a screen. The address
 * is typed, the token is fetched; the configuration key that produced it is deliberately not kept.
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

    /** Remembers what was typed even when the key was refused, so the address survives a retry. */
    suspend fun setServerUrl(url: String) {
        context.syncStore.edit { it[SERVER_URL] = url }
    }

    /** Drops the credentials. Local data is untouched: it was never the server's copy. */
    suspend fun clearConfig(keepServerUrl: Boolean) {
        context.syncStore.edit {
            it.remove(API_URL)
            it.remove(TOKEN)
            it.remove(LAST_SYNC)
            if (!keepServerUrl) it.remove(SERVER_URL)
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

    suspend fun setPalette(palette: String) {
        context.syncStore.edit { it[PALETTE] = palette }
    }

    suspend fun setAmoled(enabled: Boolean) {
        context.syncStore.edit { it[AMOLED] = enabled }
    }

    suspend fun setReminderDayBefore(enabled: Boolean) {
        context.syncStore.edit { it[REMINDER_DAY_BEFORE] = enabled }
    }

    suspend fun setReminderHourBefore(enabled: Boolean) {
        context.syncStore.edit { it[REMINDER_HOUR_BEFORE] = enabled }
    }

    suspend fun setReminderClosing(enabled: Boolean) {
        context.syncStore.edit { it[REMINDER_CLOSING] = enabled }
    }

    private fun Preferences.toState(): SyncState {
        // Written before pure black became a modifier rather than a third theme.
        val storedMode = this[THEME_MODE]
        val legacyAmoled = storedMode == LEGACY_AMOLED
        return SyncState(
        apiUrl = this[API_URL],
        token = this[TOKEN],
        serverUrl = this[SERVER_URL],
        lastSyncAt = this[LAST_SYNC]?.let(Instant::parse),
        syncOnLaunch = this[SYNC_ON_LAUNCH] ?: true,
        themeMode = when (storedMode) {
            null, LEGACY_FOLLOW_SYSTEM -> SYSTEM
            LEGACY_AMOLED -> DARK
            else -> storedMode
        },
        palette = this[PALETTE] ?: (if (this[DYNAMIC_COLOR] == true) DYNAMIC else DEFAULT),
        amoled = this[AMOLED] ?: legacyAmoled,
        reminderDayBefore = this[REMINDER_DAY_BEFORE] ?: true,
        reminderHourBefore = this[REMINDER_HOUR_BEFORE] ?: true,
        reminderClosing = this[REMINDER_CLOSING] ?: false,
        )
    }

    private companion object {
        val API_URL = stringPreferencesKey("api_url")
        val TOKEN = stringPreferencesKey("token")
        val LAST_SYNC = stringPreferencesKey("last_sync")
        val SYNC_ON_LAUNCH = booleanPreferencesKey("sync_on_launch")
        val SERVER_URL = stringPreferencesKey("server_url")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE = stringPreferencesKey("palette")
        val AMOLED = booleanPreferencesKey("amoled")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REMINDER_DAY_BEFORE = booleanPreferencesKey("reminder_day_before")
        val REMINDER_HOUR_BEFORE = booleanPreferencesKey("reminder_hour_before")
        val REMINDER_CLOSING = booleanPreferencesKey("reminder_closing")
        const val SYSTEM = "SYSTEM"
        const val DARK = "DARK"
        const val DEFAULT = "DEFAULT"
        const val DYNAMIC = "DYNAMIC"
        const val LEGACY_FOLLOW_SYSTEM = "FOLLOW_SYSTEM"
        const val LEGACY_AMOLED = "AMOLED"
    }
}

data class SyncState(
    val apiUrl: String? = null,
    val token: String? = null,
    val serverUrl: String? = null,
    val lastSyncAt: Instant? = null,
    val syncOnLaunch: Boolean = true,
    val themeMode: String = "SYSTEM",
    val palette: String = "DEFAULT",
    val amoled: Boolean = false,
    val reminderDayBefore: Boolean = true,
    val reminderHourBefore: Boolean = true,
    val reminderClosing: Boolean = false,
) {
    val isConfigured: Boolean get() = apiUrl != null && token != null
}

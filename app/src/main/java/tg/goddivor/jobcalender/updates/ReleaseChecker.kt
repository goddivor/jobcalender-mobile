package tg.goddivor.jobcalender.updates

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import tg.goddivor.jobcalender.BuildConfig
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateStore by preferencesDataStore(name = "updates")

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
)

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val apkUrl: String?,
    val pageUrl: String,
    val sizeBytes: Long,
) {
    val isNewerThanInstalled: Boolean
        get() = compareVersions(version, BuildConfig.VERSION_NAME) > 0
}

/**
 * Checks GitHub Releases for a newer APK. Never throws: offline, rate-limited or malformed, it
 * returns null and the app carries on. Distribution is by release, not by the Play Store.
 */
@Singleton
class ReleaseChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(force: Boolean = false): ReleaseInfo? {
        if (!force) {
            cached()?.let { return it }
        }
        val fetched = runCatching { fetch() }.getOrNull() ?: return cached()
        store(fetched)
        return fetched
    }

    /** The startup check stays silent about a version the user already waved away. */
    suspend fun startupUpdate(): ReleaseInfo? {
        val release = check() ?: return null
        if (!release.isNewerThanInstalled) return null
        val dismissed = context.updateStore.data.first()[DISMISSED]
        return release.takeIf { it.version != dismissed }
    }

    suspend fun dismiss(version: String) {
        context.updateStore.edit { it[DISMISSED] = version }
    }

    private suspend fun cached(): ReleaseInfo? {
        val stored = context.updateStore.data.first()
        val fetchedAt = stored[FETCHED_AT]?.let(Instant::parse) ?: return null
        if (Duration.between(fetchedAt, Instant.now()) > CACHE_LIFETIME) return null
        val version = stored[VERSION] ?: return null
        return ReleaseInfo(
            version = version,
            notes = stored[NOTES].orEmpty(),
            apkUrl = stored[APK_URL],
            pageUrl = stored[PAGE_URL] ?: RELEASES_PAGE,
            sizeBytes = stored[SIZE]?.toLongOrNull() ?: 0,
        )
    }

    private suspend fun store(release: ReleaseInfo) {
        context.updateStore.edit {
            it[VERSION] = release.version
            it[NOTES] = release.notes
            release.apkUrl?.let { url -> it[APK_URL] = url }
            it[PAGE_URL] = release.pageUrl
            it[SIZE] = release.sizeBytes.toString()
            it[FETCHED_AT] = Instant.now().toString()
        }
    }

    private fun fetch(): ReleaseInfo? {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            val release = json.decodeFromString<GithubRelease>(body)
            val version = release.tagName.removePrefix("v").ifBlank { return null }
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            return ReleaseInfo(
                version = version,
                notes = markdownToText(release.body),
                apkUrl = apk?.downloadUrl,
                pageUrl = release.htmlUrl.ifBlank { RELEASES_PAGE },
                sizeBytes = apk?.size ?: 0,
            )
        }
    }

    private companion object {
        const val REPOSITORY = "goddivor/jobcalender-mobile"
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases"
        val CACHE_LIFETIME: Duration = Duration.ofHours(6)

        val VERSION = stringPreferencesKey("version")
        val NOTES = stringPreferencesKey("notes")
        val APK_URL = stringPreferencesKey("apk_url")
        val PAGE_URL = stringPreferencesKey("page_url")
        val SIZE = stringPreferencesKey("size")
        val FETCHED_AT = stringPreferencesKey("fetched_at")
        val DISMISSED = stringPreferencesKey("dismissed_version")
    }
}

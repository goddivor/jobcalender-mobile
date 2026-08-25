package tg.goddivor.jobcalender.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : InstallState
    data object ReadyToInstall : InstallState
    data object PermissionNeeded : InstallState
    data class Failed(val reason: String) : InstallState
}

/**
 * Downloads the release APK and hands it to the system installer. Distribution is by GitHub
 * release, so the app installs its own updates, which needs REQUEST_INSTALL_PACKAGES and a
 * FileProvider: a raw file:// URI has been rejected since Android 7.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun canInstall(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openInstallPermissionSettings() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun enqueue(release: ReleaseInfo): Long? {
        val url = release.apkUrl ?: return null
        val fileName = "jobcalender-${release.version}.apk"

        // Clear a partial file from an interrupted attempt: DownloadManager would otherwise append.
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName).delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("JobCalender ${release.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType(APK_MIME)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return runCatching { manager.enqueue(request) }.getOrNull()
    }

    fun progress(downloadId: Long): InstallState {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return InstallState.Idle
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> InstallState.ReadyToInstall
                DownloadManager.STATUS_FAILED -> InstallState.Failed("download")
                else -> InstallState.Downloading(done, total)
            }
        }
    }

    fun install(version: String): Boolean {
        if (!canInstall()) return false
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "jobcalender-$version.apk",
        )
        if (!file.exists()) return false

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}

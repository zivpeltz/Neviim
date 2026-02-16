package com.neviim.market.data.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub for new releases and downloads + installs the APK
 * if a newer version is available.
 *
 * Release convention:
 *   - Each GitHub release has a tag like "v1.0", "v1.1", etc.
 *   - The release must contain an .apk asset.
 */
object AppUpdater {

    private const val REPO_OWNER = "zivpeltz"
    private const val REPO_NAME = "Neviim"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    private const val COMMITS_API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/commits?per_page=1"

    sealed class UpdateResult {
        data class Available(
            val tagName: String,
            val releaseName: String,
            val downloadUrl: String?,
            val body: String
        ) : UpdateResult()

        data object UpToDate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

    sealed class CommitCheckResult {
        data class NewCommit(
            val sha: String,
            val message: String,
            val date: String
        ) : CommitCheckResult()

        data object UpToDate : CommitCheckResult()
        data class Error(val message: String) : CommitCheckResult()
    }

    /**
     * Check if there's a newer release on GitHub compared to the current version.
     */
    suspend fun checkForRelease(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext UpdateResult.UpToDate
            }
            if (responseCode != 200) {
                return@withContext UpdateResult.Error("GitHub API error: $responseCode")
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "")
            val releaseName = json.optString("name", tagName)
            val releaseBody = json.optString("body", "")

            // Compare versions
            val remoteVersion = tagName.removePrefix("v").trim()
            val localVersion = currentVersionName.removePrefix("v").trim()

            if (remoteVersion == localVersion || remoteVersion.isEmpty()) {
                return@withContext UpdateResult.UpToDate
            }

            // Look for an APK asset
            val assets = json.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }

            UpdateResult.Available(
                tagName = tagName,
                releaseName = releaseName,
                downloadUrl = apkUrl,
                body = releaseBody
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Check the latest commit on main. Compare with a locally stored SHA
     * to see if the repo has changed since the last check.
     */
    suspend fun checkForNewCommits(context: Context): CommitCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(COMMITS_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return@withContext CommitCheckResult.Error("GitHub API error: $responseCode")
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val arr = JSONArray(body)
            if (arr.length() == 0) {
                return@withContext CommitCheckResult.UpToDate
            }

            val latestCommit = arr.getJSONObject(0)
            val sha = latestCommit.optString("sha", "")
            val commitObj = latestCommit.optJSONObject("commit")
            val message = commitObj?.optString("message", "") ?: ""
            val date = commitObj?.optJSONObject("committer")?.optString("date", "") ?: ""

            // Read stored SHA
            val prefs = context.getSharedPreferences("neviim_updater", Context.MODE_PRIVATE)
            val storedSha = prefs.getString("last_sha", "") ?: ""

            if (sha == storedSha) {
                return@withContext CommitCheckResult.UpToDate
            }

            // Store new SHA
            prefs.edit().putString("last_sha", sha).apply()

            // If this is the first time (no stored SHA), treat as up-to-date
            if (storedSha.isEmpty()) {
                return@withContext CommitCheckResult.UpToDate
            }

            CommitCheckResult.NewCommit(
                sha = sha.take(7),
                message = message.lines().firstOrNull() ?: message,
                date = date
            )
        } catch (e: Exception) {
            CommitCheckResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Download an APK file using DownloadManager and trigger install.
     */
    fun downloadAndInstall(context: Context, downloadUrl: String, fileName: String = "neviim-update.apk") {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Neviim Update")
            .setDescription("Downloading latest version…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }
}

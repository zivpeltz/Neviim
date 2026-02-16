package com.neviim.market.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.repository.SettingsRepository
import com.neviim.market.data.updater.AppUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val themeMode: StateFlow<SettingsRepository.ThemeMode> = SettingsRepository.themeMode

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    val appVersion: String = try {
        val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        pInfo.versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }

    fun setThemeMode(mode: SettingsRepository.ThemeMode) {
        SettingsRepository.setThemeMode(mode)
    }

    fun checkForUpdates() {
        _updateStatus.value = UpdateStatus.Checking

        viewModelScope.launch {
            // First try release-based check
            val releaseResult = AppUpdater.checkForRelease(appVersion)

            when (releaseResult) {
                is AppUpdater.UpdateResult.Available -> {
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        version = releaseResult.tagName,
                        description = releaseResult.body,
                        downloadUrl = releaseResult.downloadUrl
                    )
                    return@launch
                }
                is AppUpdater.UpdateResult.Error -> {
                    // Fall through to commit check
                }
                is AppUpdater.UpdateResult.UpToDate -> {
                    // Fall through to commit check
                }
            }

            // Then try commit-based check
            val commitResult = AppUpdater.checkForNewCommits(getApplication())

            _updateStatus.value = when (commitResult) {
                is AppUpdater.CommitCheckResult.NewCommit -> {
                    UpdateStatus.NewCommitAvailable(
                        sha = commitResult.sha,
                        message = commitResult.message
                    )
                }
                is AppUpdater.CommitCheckResult.UpToDate -> {
                    UpdateStatus.UpToDate
                }
                is AppUpdater.CommitCheckResult.Error -> {
                    UpdateStatus.Error(commitResult.message)
                }
            }
        }
    }

    fun downloadUpdate(url: String) {
        AppUpdater.downloadAndInstall(getApplication(), url)
        _updateStatus.value = UpdateStatus.Downloading
    }

    fun dismissUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    sealed class UpdateStatus {
        data object Idle : UpdateStatus()
        data object Checking : UpdateStatus()
        data object UpToDate : UpdateStatus()
        data object Downloading : UpdateStatus()
        data class UpdateAvailable(
            val version: String,
            val description: String,
            val downloadUrl: String?
        ) : UpdateStatus()
        data class NewCommitAvailable(
            val sha: String,
            val message: String
        ) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }
}

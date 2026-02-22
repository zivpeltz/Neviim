package com.neviim.market.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neviim.market.R
import com.neviim.market.data.repository.SettingsRepository
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDevRefill by remember { mutableStateOf(false) }
    var devRefillInput by remember { mutableStateOf("") }
    val devRefillMessage by viewModel.devRefillMessage.collectAsState()
    val devSnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(devRefillMessage) {
        devRefillMessage?.let {
            devSnackbarHostState.showSnackbar(it)
            viewModel.clearDevRefillMessage()
        }
    }

    // ── Theme Dialog ──────────────────────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    SettingsRepository.ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (mode) {
                                    SettingsRepository.ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    SettingsRepository.ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    SettingsRepository.ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.settings_dismiss))
                }
            }
        )
    }

    // ── About Dialog ──────────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                    Text(
                        text = "${stringResource(R.string.about_version)}: ${viewModel.appVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stringResource(R.string.about_developer)}: Ziv Peltz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.about_github_url),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.settings_dismiss))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(devSnackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Appearance Section ──────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))

            SettingsRow(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = when (themeMode) {
                    SettingsRepository.ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    SettingsRepository.ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    SettingsRepository.ThemeMode.DARK -> stringResource(R.string.theme_dark)
                },
                onClick = { showThemeDialog = true }
            )

            SettingsRow(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                subtitle = stringResource(R.string.settings_language_subtitle),
                onClick = {
                    // Open Android language settings
                    val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // ── Updates Section ─────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_updates))

            SettingsRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.settings_check_updates),
                subtitle = when (updateStatus) {
                    is SettingsViewModel.UpdateStatus.Idle ->
                        stringResource(R.string.settings_check_updates_subtitle)
                    is SettingsViewModel.UpdateStatus.Checking ->
                        stringResource(R.string.update_checking)
                    is SettingsViewModel.UpdateStatus.UpToDate ->
                        stringResource(R.string.update_up_to_date)
                    is SettingsViewModel.UpdateStatus.Downloading ->
                        stringResource(R.string.update_downloading)
                    is SettingsViewModel.UpdateStatus.UpdateAvailable -> {
                        val status = updateStatus as SettingsViewModel.UpdateStatus.UpdateAvailable
                        "${stringResource(R.string.update_available)}: ${status.version}"
                    }
                    is SettingsViewModel.UpdateStatus.NewCommitAvailable -> {
                        val status = updateStatus as SettingsViewModel.UpdateStatus.NewCommitAvailable
                        "${stringResource(R.string.update_new_commit)}: ${status.message}"
                    }
                    is SettingsViewModel.UpdateStatus.Error -> {
                        val status = updateStatus as SettingsViewModel.UpdateStatus.Error
                        "${stringResource(R.string.update_error)}: ${status.message}"
                    }
                },
                onClick = { viewModel.checkForUpdates() },
                showProgress = updateStatus is SettingsViewModel.UpdateStatus.Checking
            )

            // Show download button when update is available
            if (updateStatus is SettingsViewModel.UpdateStatus.UpdateAvailable) {
                val status = updateStatus as SettingsViewModel.UpdateStatus.UpdateAvailable
                if (status.downloadUrl != null) {
                    Button(
                        onClick = { viewModel.downloadUpdate(status.downloadUrl!!) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenProfit
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.update_download),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Show "View Update" button when a new commit is available
            if (updateStatus is SettingsViewModel.UpdateStatus.NewCommitAvailable) {
                Button(
                    onClick = { viewModel.openReleasesPage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_view_release),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // ── Developer Section ───────────────────────────────────
            SettingsSectionHeader("Developer")

            // Expandable row for adding SP
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDevRefill = !showDevRefill },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.animateContentSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add ShekelPoints",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dev only — add SP to your balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (showDevRefill) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    AnimatedVisibility(visible = showDevRefill) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = devRefillInput,
                                onValueChange = { devRefillInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Amount to add") },
                                placeholder = { Text("e.g. 50000") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Button(
                                onClick = {
                                    val amount = devRefillInput.toDoubleOrNull()
                                    if (amount != null && amount > 0) {
                                        viewModel.refillBalance(amount)
                                        devRefillInput = ""
                                        showDevRefill = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = (devRefillInput.toDoubleOrNull() ?: 0.0) > 0
                            ) {
                                Text("Add SP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // ── About Section ───────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about),
                subtitle = "${stringResource(R.string.about_version)} ${viewModel.appVersion}",
                onClick = { showAboutDialog = true }
            )

            SettingsRow(
                icon = Icons.Default.Code,
                title = stringResource(R.string.settings_source_code),
                subtitle = "github.com/zivpeltz/Neviim",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zivpeltz/Neviim"))
                    context.startActivity(intent)
                }
            )

            SettingsRow(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_subtitle),
                onClick = { }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Footer ──────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_footer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Reusable Settings Components ─────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showProgress: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

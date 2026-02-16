package com.neviim.market.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neviim.market.R
import com.neviim.market.data.model.EventTag
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.CreateEventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    viewModel: CreateEventViewModel = viewModel()
) {
    val title by viewModel.title.collectAsState()
    val titleHe by viewModel.titleHe.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val probability by viewModel.initialProbability.collectAsState()
    val created by viewModel.created.collectAsState()

    val tagStringMap = mapOf(
        EventTag.POLITICS to stringResource(R.string.tag_politics),
        EventTag.POP_CULTURE to stringResource(R.string.tag_pop_culture),
        EventTag.CRYPTO to stringResource(R.string.tag_crypto),
        EventTag.SCIENCE to stringResource(R.string.tag_science),
        EventTag.SPORTS to stringResource(R.string.tag_sports)
    )

    // Navigate back when created
    LaunchedEffect(created) {
        if (created) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_event_title),
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Event Title (English) ───────────────────────────────
            Text(
                text = stringResource(R.string.create_event_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.create_event_name_hint)) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // ── Event Title (Hebrew) ────────────────────────────────
            Text(
                text = stringResource(R.string.create_event_name_he),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = titleHe,
                onValueChange = { viewModel.updateTitleHe(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.create_event_name_he_hint)) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // ── Tag Selection ───────────────────────────────────────
            Text(
                text = stringResource(R.string.create_event_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventTag.entries.forEach { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.selectTag(tag) },
                        label = {
                            Text(
                                text = tagStringMap[tag] ?: tag.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── Initial Probability Slider ──────────────────────────
            Text(
                text = stringResource(R.string.create_event_probability),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${probability.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.yes_label)} ${probability.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = YesColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stringResource(R.string.no_label)} ${(100 - probability.toInt())}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = probability,
                        onValueChange = { viewModel.updateProbability(it) },
                        valueRange = 5f..95f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = YesColor,
                            inactiveTrackColor = NoColor.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Create Button ───────────────────────────────────────
            Button(
                onClick = { viewModel.createEvent() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = viewModel.canCreate(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Text(
                    text = stringResource(R.string.create_event_submit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

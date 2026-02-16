package com.neviim.market.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.neviim.market.data.model.EventType
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    viewModel: CreateEventViewModel = viewModel()
) {
    val title by viewModel.title.collectAsState()
    val titleHe by viewModel.titleHe.collectAsState()
    val description by viewModel.description.collectAsState()
    val descriptionHe by viewModel.descriptionHe.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val eventType by viewModel.eventType.collectAsState()
    val probability by viewModel.initialProbability.collectAsState()
    val options by viewModel.options.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val created by viewModel.created.collectAsState()

    val tagStringMap = mapOf(
        EventTag.POLITICS to stringResource(R.string.tag_politics),
        EventTag.POP_CULTURE to stringResource(R.string.tag_pop_culture),
        EventTag.CRYPTO to stringResource(R.string.tag_crypto),
        EventTag.SCIENCE to stringResource(R.string.tag_science),
        EventTag.SPORTS to stringResource(R.string.tag_sports)
    )

    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate ?: (System.currentTimeMillis() + 86_400_000L * 30)
    )
    var showDatePicker by remember { mutableStateOf(false) }

    // Navigate back when created
    LaunchedEffect(created) {
        if (created) onBack()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateEndDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm_label))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateEndDate(null)
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.no_end_date_label))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            // ── Event Type Toggle ──────────────────────────────────
            Text(
                text = stringResource(R.string.create_event_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = eventType == EventType.BINARY,
                    onClick = { viewModel.selectEventType(EventType.BINARY) },
                    label = { Text(stringResource(R.string.type_binary)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = eventType == EventType.MULTI_CHOICE,
                    onClick = { viewModel.selectEventType(EventType.MULTI_CHOICE) },
                    label = { Text(stringResource(R.string.type_multi_choice)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

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

            // ── Description ────────────────────────────────────────
            Text(
                text = stringResource(R.string.create_event_description),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.updateDescription(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.create_event_description_hint)) },
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                maxLines = 4,
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

            // ── End Date ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.create_event_end_date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (endDate != null) {
                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            .format(Date(endDate!!))
                    } else {
                        stringResource(R.string.select_end_date)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // ── Binary: Probability Slider ──────────────────────────
            if (eventType == EventType.BINARY) {
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
            }

            // ── Multi-choice: Options ───────────────────────────────
            if (eventType == EventType.MULTI_CHOICE) {
                Text(
                    text = stringResource(R.string.create_event_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                options.forEachIndexed { index, (english, hebrew) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(R.string.option_label)} ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (options.size > 2) {
                                    IconButton(
                                        onClick = { viewModel.removeOption(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_option),
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = english,
                                onValueChange = { viewModel.updateOption(index, it, hebrew) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.option_en_hint)) },
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = hebrew,
                                onValueChange = { viewModel.updateOption(index, english, it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.option_he_hint)) },
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }

                // Add option button
                if (options.size < 8) {
                    OutlinedButton(
                        onClick = { viewModel.addOption() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_option))
                    }
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

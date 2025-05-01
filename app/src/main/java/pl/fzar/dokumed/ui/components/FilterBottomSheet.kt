package pl.fzar.dokumed.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.MedicalRecordType
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    allTypes: List<MedicalRecordType>,
    selectedTypes: Set<MedicalRecordType>,
    onTypesChange: (Set<MedicalRecordType>) -> Unit,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    allTags: List<String>,
    selectedTags: Set<String>,
    onTagsChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Type filters
            Text(text = stringResource(R.string.type_of_exam), style = MaterialTheme.typography.titleMedium)
            allTypes.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { type ->
                        val checked = selectedTypes.contains(type)
                        FilterChip(
                            selected = checked,
                            onClick = {
                                val newSet = selectedTypes.toMutableSet().apply {
                                    if (checked) remove(type) else add(type)
                                }
                                onTypesChange(newSet)
                            },
                            label = { Text(type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date range
            Text(text = stringResource(R.string.date_range), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                val dateFromInteraction = remember { MutableInteractionSource() }
                OutlinedTextField(
                    value = dateFrom?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.from)) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    enabled = true,
                    singleLine = true,
                    interactionSource = dateFromInteraction,
                )
                val dateToInteraction = remember { MutableInteractionSource() }
                OutlinedTextField(
                    value = dateTo?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.to)) },
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    enabled = true,
                    singleLine = true,
                    interactionSource = dateToInteraction,
                )
                LaunchedEffect(dateFromInteraction) {
                    dateFromInteraction.interactions.collect { interaction ->
                        if (interaction is FocusInteraction.Focus) {
                            showDateFromPicker = true
                        }
                    }
                }
                LaunchedEffect(dateToInteraction) {
                    dateToInteraction.interactions.collect { interaction ->
                        if (interaction is FocusInteraction.Focus) {
                            showDateToPicker = true
                        }
                    }
                }
            }
            if (showDateFromPicker) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = dateFrom?.toJavaLocalDate()?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDateFromPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = state.selectedDateMillis
                            val localDate = millis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            onDateRangeChange(localDate?.let { kotlinx.datetime.LocalDate(it.year, it.monthValue, it.dayOfMonth) }, dateTo)
                            showDateFromPicker = false
                        }) { Text(stringResource(R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDateFromPicker = false }) { Text(stringResource(R.string.cancel)) }
                    }
                ) {
                    DatePicker(state = state)
                }
            }
            if (showDateToPicker) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = dateTo?.toJavaLocalDate()?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDateToPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = state.selectedDateMillis
                            val localDate = millis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            onDateRangeChange(dateFrom, localDate?.let { kotlinx.datetime.LocalDate(it.year, it.monthValue, it.dayOfMonth) })
                            showDateToPicker = false
                        }) { Text(stringResource(R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDateToPicker = false }) { Text(stringResource(R.string.cancel)) }
                    }
                ) {
                    DatePicker(state = state)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag filters
            Text(text = stringResource(R.string.tags), style = MaterialTheme.typography.titleMedium)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                allTags.forEach { tag ->
                    val checked = selectedTags.contains(tag)
                    FilterChip(
                        selected = checked,
                        onClick = {
                            val newSet = selectedTags.toMutableSet().apply {
                                if (checked) remove(tag) else add(tag)
                            }
                            onTagsChange(newSet)
                        },
                        label = { Text(tag) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

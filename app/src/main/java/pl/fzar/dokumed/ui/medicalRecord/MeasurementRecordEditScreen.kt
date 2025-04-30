package pl.fzar.dokumed.ui.medicalRecord

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.model.MeasurementRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.getLocalizedString
import pl.fzar.dokumed.ui.theme.DokumedTheme
import java.util.*
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementRecordEditScreen(
    measurementRecord: MeasurementRecord,
    onBackClick: () -> Unit,
    onRecordEdited: (MeasurementRecord) -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(measurementRecord.date) }
    var type by remember { mutableStateOf(measurementRecord.type) }
    var description by remember { mutableStateOf(measurementRecord.description ?: "") }
    var notes by remember { mutableStateOf(measurementRecord.notes ?: "") }
    var tags by remember { mutableStateOf(measurementRecord.tags) }
    var newTag by remember { mutableStateOf("") }
    var testName by remember { mutableStateOf(measurementRecord.testName) }
    var valueText by remember { mutableStateOf(measurementRecord.value?.toString() ?: "") }
    var unit by remember { mutableStateOf(measurementRecord.unit ?: "") }
    var expandedTypeSelector by remember { mutableStateOf(false) }

    val isSaveEnabled = testName.isNotBlank() && valueText.toDoubleOrNull() != null && unit.isNotBlank()

    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = LocalDate(year, month + 1, dayOfMonth)
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edytuj pomiar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Data
            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text("Data") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Zmień datę")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Typ
            ExposedDropdownMenuBox(
                expanded = expandedTypeSelector,
                onExpandedChange = { expandedTypeSelector = !expandedTypeSelector },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = type.getLocalizedString(context),
                    onValueChange = { },
                    label = { Text("Typ") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTypeSelector) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedTypeSelector,
                    onDismissRequest = { expandedTypeSelector = false }
                ) {
                    MedicalRecordType.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.getLocalizedString(context)) },
                            onClick = {
                                type = item
                                expandedTypeSelector = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Opis
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Notatki
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notatki") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Nazwa badania
            OutlinedTextField(
                value = testName,
                onValueChange = { testName = it },
                label = { Text("Nazwa badania *") },
                modifier = Modifier.fillMaxWidth(),
                isError = testName.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Wartość
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Wartość *") },
                modifier = Modifier.fillMaxWidth(),
                isError = valueText.toDoubleOrNull() == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Jednostka
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("Jednostka *") },
                modifier = Modifier.fillMaxWidth(),
                isError = unit.isBlank()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tagi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("Dodaj tag") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newTag.isNotBlank()) {
                            tags = tags + newTag
                            newTag = ""
                        }
                    },
                    enabled = newTag.isNotBlank()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj tag")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Tagi:", style = MaterialTheme.typography.bodyMedium)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = { tags.filter { it != tag } },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Usuń tag",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Zapisz
            Button(
                onClick = {
                    val updatedRecord = measurementRecord.copy(
                        date = date,
                        type = type,
                        description = description,
                        notes = notes,
                        tags = tags,
                        testName = testName,
                        value = valueText.toDoubleOrNull(),
                        unit = unit
                    )
                    onRecordEdited(updatedRecord)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            ) {
                Text("Zapisz zmiany")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementRecordEditScreenPreview() {
    DokumedTheme {
        MeasurementRecordEditScreen(
            measurementRecord = MeasurementRecord(
                id = Uuid.random(),
                date = LocalDate(2025, 4, 26),
                type = MedicalRecordType.MEASUREMENT,
                description = "Pomiar cukru we krwi",
                notes = "Na czczo",
                tags = listOf("glukoza", "badanie"),
                testName = "Glukoza",
                value = 90.0,
                unit = "mg/dL"
            ),
            onBackClick = {},
            onRecordEdited = {}
        )
    }
}
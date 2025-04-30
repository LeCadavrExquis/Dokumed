package pl.fzar.dokumed.ui.medicalRecord

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.clinicalDataRecords
import pl.fzar.dokumed.data.model.consultationRecords
import pl.fzar.dokumed.data.model.dummyRecords
import pl.fzar.dokumed.data.model.getLocalizedString
import pl.fzar.dokumed.data.model.measurementRecords
import java.util.Calendar
import kotlin.uuid.Uuid


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordEditScreen(
    medicalRecord: MedicalRecord?,
    onBackClick: () -> Unit,
    onRecordEdited: (MedicalRecord) -> Unit,
    copyFileToLocalStorage: (Context, Uri, String) -> Unit,
    onDeleteRecord: ((MedicalRecord) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Default values for new record
    val isNewRecord = medicalRecord == null
    val initialDate = medicalRecord?.date ?: LocalDate(2025, 4, 30) // Current date
    val initialType = medicalRecord?.type ?: MedicalRecordType.CONSULTATION
    
    var date by remember { mutableStateOf(initialDate) }
    var type by remember { mutableStateOf(initialType) }
    var expandedTypeSelector by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(medicalRecord?.description ?: "") }
    var notes by remember { mutableStateOf(medicalRecord?.notes ?: "") }
    var tags by remember { mutableStateOf(medicalRecord?.tags ?: emptySet()) }
    var newTag by remember { mutableStateOf("") }
    
    // State for delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Type-specific fields
    var doctor by remember { mutableStateOf((medicalRecord as? ConsultationRecord)?.doctor ?: "") }
    var filePath by remember { mutableStateOf((medicalRecord as? ConsultationRecord)?.filePath ?: (medicalRecord as? ClinicalDataRecord)?.filePath) }
    var fileMimeType by remember { mutableStateOf((medicalRecord as? ConsultationRecord)?.fileMimeType ?: (medicalRecord as? ClinicalDataRecord)?.fileMimeType) }
    var testName by remember { mutableStateOf((medicalRecord as? MeasurementRecord)?.testName ?: (medicalRecord as? ClinicalDataRecord)?.testName ?: "") }
    var valueText by remember { mutableStateOf((medicalRecord as? MeasurementRecord)?.value?.toString() ?: "") }
    var unit by remember { mutableStateOf((medicalRecord as? MeasurementRecord)?.unit ?: "") }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = LocalDate(year, month + 1, dayOfMonth)
            },
            date.year,
            date.monthNumber - 1,
            date.dayOfMonth
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "uploaded_file"
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (columnIndex != -1) {
                            fileName = it.getString(columnIndex)
                        }
                    }
                }
            }
            copyFileToLocalStorage(context, it, fileName)
            filePath = it.toString()
            fileMimeType = context.contentResolver.getType(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edytuj rekord medyczny") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    // Only show the delete button if we have a callback and this is not a new record
                    if (onDeleteRecord != null && !isNewRecord) {
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                        ) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "Usuń rekord",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val record = when (type) {
                        in consultationRecords -> ConsultationRecord(
                            id = medicalRecord?.id ?: Uuid.random(),
                            date = date,
                            type = type,
                            description = description,
                            notes = notes,
                            tags = tags.toList(),
                            doctor = doctor,
                            filePath = filePath,
                            fileMimeType = fileMimeType
                        )
                        in measurementRecords -> MeasurementRecord(
                            id = medicalRecord?.id ?: Uuid.random(),
                            date = date,
                            type = type,
                            description = description,
                            notes = notes,
                            tags = tags.toList(),
                            testName = testName,
                            value = valueText.toDoubleOrNull(),
                            unit = unit
                        )
                        in clinicalDataRecords -> ClinicalDataRecord(
                            id = medicalRecord?.id ?: Uuid.random(), // Generate new ID if null
                            date = date,
                            type = type,
                            description = description,
                            notes = notes,
                            tags = tags.toList(),
                            testName = testName,
                            filePath = filePath,
                            fileMimeType = fileMimeType
                        )
                        else -> throw IllegalArgumentException("Invalid record type")
                    }
                    onRecordEdited(record)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.save),
                    contentDescription = "Zapisz",
                    modifier = Modifier.size(24.dp)
                )
            }
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
                        Icon(Icons.Filled.Add, contentDescription = "Wybierz datę")
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            // Typ
            ExposedDropdownMenuBox(
                expanded = expandedTypeSelector,
                onExpandedChange = { expandedTypeSelector = !expandedTypeSelector },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = type.getLocalizedString(context),
                    onValueChange = { },
                    label = { Text("Typ") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTypeSelector) },
                    modifier = Modifier.menuAnchor(),
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

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notatki") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

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

            FlowRow(modifier = Modifier.fillMaxWidth()) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = { tags.filter { it !=tag } },
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
            Spacer(modifier = Modifier.height(8.dp))

            // Type-specific fields
            when (type) {
                in consultationRecords -> {
                    OutlinedTextField(
                        value = doctor,
                        onValueChange = { doctor = it },
                        label = { Text("Lekarz") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // File attachment
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { filePickerLauncher.launch("*") }) {
                            Text("Załącz plik")
                        }
                        Spacer(Modifier.width(8.dp))
                        filePath?.let {
                            Text(it.takeLast(30), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                in measurementRecords -> {
                    OutlinedTextField(
                        value = testName,
                        onValueChange = { testName = it },
                        label = { Text("Nazwa testu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = { Text("Wartość") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Jednostka") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                in clinicalDataRecords -> {
                    OutlinedTextField(
                        value = testName,
                        onValueChange = { testName = it },
                        label = { Text("Nazwa badania") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // File attachment
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { filePickerLauncher.launch("*") }) {
                            Text("Załącz plik")
                        }
                        Spacer(Modifier.width(8.dp))
                        filePath?.let {
                            Text(it.takeLast(30), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                else -> {}
            }

            // Add some extra space at the bottom to ensure content isn't hidden behind the FAB
            Spacer(Modifier.height(80.dp))

            // Delete confirmation dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Potwierdzenie usunięcia") },
                    text = { Text("Czy na pewno chcesz usunąć ten rekord? Tej operacji nie można cofnąć.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                onDeleteRecord?.invoke(medicalRecord!!)
                                onBackClick()
                            }
                        ) {
                            Text("Usuń", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMedicalRecordEditScreen() {
    MedicalRecordEditScreen(
        medicalRecord = dummyRecords[7],
        onBackClick = {},
        onRecordEdited = {},
        copyFileToLocalStorage = { _, _, _ -> },
        onDeleteRecord = { }
    )
}

@Preview(showBackground = true, name = "New Record")
@Composable
fun PreviewNewMedicalRecordEditScreen() {
    MedicalRecordEditScreen(
        medicalRecord = null, // New record
        onBackClick = {},
        onRecordEdited = {},
        copyFileToLocalStorage = { _, _, _ -> },
        onDeleteRecord = null
    )
}
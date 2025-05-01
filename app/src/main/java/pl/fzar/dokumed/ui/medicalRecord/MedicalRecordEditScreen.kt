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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.Measurement
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.clinicalDataRecords
import pl.fzar.dokumed.data.model.consultationRecords
import pl.fzar.dokumed.data.model.dummyRecords
import pl.fzar.dokumed.data.model.getLocalizedString
import pl.fzar.dokumed.data.model.measurementRecords
import kotlin.uuid.Uuid


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordEditScreen(
    medicalRecord: MedicalRecord?,
    onBackClick: () -> Unit,
    onRecordEdited: (MedicalRecord) -> Unit,
    copyFileToLocalStorage: (Context, Uri, String, (String) -> Unit) -> Unit,
    onDeleteRecord: ((MedicalRecord) -> Unit)? = null,
    pendingAttachment: ClinicalData? = null,
    consumesPendingAttachment: () -> Unit,
) {
    val context = LocalContext.current
    
    // Default values for new record
    val isNewRecord = medicalRecord == null
    val initialDate = medicalRecord?.date ?: LocalDate(2025, 4, 30)
    val initialType = medicalRecord?.type ?: MedicalRecordType.CONSULTATION
    
    var date by remember { mutableStateOf(initialDate) }
    var type by remember { mutableStateOf(initialType) }
    var expandedTypeSelector by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(medicalRecord?.description ?: "") }
    var doctor by remember { mutableStateOf(medicalRecord?.doctor ?: "") }
    var notes by remember { mutableStateOf(medicalRecord?.notes ?: "") }
    var tags by remember { mutableStateOf(medicalRecord?.tags?.toSet() ?: emptySet()) }
    var newTag by remember { mutableStateOf("") }
    
    // Clinical Data fields (can now hold multiple files)
    var clinicalDataList by remember {
        mutableStateOf(medicalRecord?.clinicalData ?: emptyList())
    }

    // Measurement fields (for MEASUREMENT, MEDICATION, SYMPTOM)
    var measurement by remember {
        // Initialize with a non-null Measurement object, even for new records
        mutableStateOf(medicalRecord?.measurements?.firstOrNull() ?: Measurement())
    }

    // State for delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
            var fileName = "uploaded_file_${Uuid.random()}" // Ensure unique default name
            val mimeType = context.contentResolver.getType(it) // Get MIME type
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
            copyFileToLocalStorage(context, it, fileName) { savedFilePath ->
                // Create a new ClinicalData object for the uploaded file
                val newClinicalData = ClinicalData(
                    filePath = savedFilePath,
                    fileMimeType = mimeType,
                    fileName = fileName // Store the original filename
                )
                // Add the new file to the list
                clinicalDataList = clinicalDataList + newClinicalData
            }
        }
    }

    // Handle pending attachment for new records
    LaunchedEffect(isNewRecord, pendingAttachment) {
        if (isNewRecord && pendingAttachment != null) {
            // Only add if not already present (e.g., due to config change)
            if (clinicalDataList.none { it.filePath == pendingAttachment!!.filePath }) {
                clinicalDataList = clinicalDataList + pendingAttachment!!
            }
            consumesPendingAttachment()// Consume it after adding
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
                    // Ensure measurement is handled safely
                    val currentMeasurement = measurement // Capture current state

                    val measurementsList = if (type in measurementRecords && currentMeasurement != null) {
                        // Use safe access or provide defaults if needed
                        listOf(currentMeasurement.copy(value = currentMeasurement.value?.toString()?.toDoubleOrNull()))
                    } else {
                        emptyList()
                    }

                    // Use the clinicalDataList directly if the type supports it
                    val finalClinicalDataList = if (type in clinicalDataRecords || type in consultationRecords) {
                        clinicalDataList
                    } else {
                        emptyList()
                    }

                    // Create or update the record
                    val updatedRecord = if (isNewRecord) {
                        MedicalRecord(
                            id = Uuid.random(),
                            date = date,
                            type = type,
                            description = description,
                            notes = notes,
                            tags = tags.toList(),
                            measurements = measurementsList,
                            clinicalData = finalClinicalDataList, // Use the potentially updated list
                            doctor = doctor
                        )
                    } else {
                        medicalRecord!!.copy(
                            date = date,
                            type = type,
                            description = description,
                            notes = notes,
                            tags = tags.toList(),
                            measurements = measurementsList,
                            clinicalData = finalClinicalDataList, // Use the potentially updated list
                            doctor = doctor
                        )
                    }

                    onRecordEdited(updatedRecord)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_save),
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

            OutlinedTextField(
                value = doctor ?: "",
                onValueChange = { doctor = it },
                label = { Text("Lekarz") },
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
                        onClick = { tags = tags.filter { it != tag }.toSet() },
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
                in consultationRecords, in clinicalDataRecords -> {
                    // Section for managing multiple files
                    Text("Załączone pliki:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    // List of attached files
                    if (clinicalDataList.isNotEmpty()) {
                        // Use LazyColumn or Column based on expected number of files
                        Column { // Changed from LazyColumn for simplicity if list is short
                            clinicalDataList.forEach { fileData -> // Iterate directly
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = fileData.fileName ?: fileData.filePath?.substringAfterLast('/') ?: "Nieznany plik",
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            clinicalDataList = clinicalDataList.filter { it.id != fileData.id && it.filePath != fileData.filePath } // Ensure correct removal
                                            // Optional: Delete the actual file from storage here if needed
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Usuń plik")
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Brak załączonych plików.")
                    }

                    Spacer(Modifier.height(8.dp))

                    // Button to add a new file
                    Button(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Dodaj plik", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dodaj plik")
                    }
                    Spacer(Modifier.height(16.dp)) // Add space after file section
                }
                in measurementRecords -> {
                    measurement?.let { m -> // Use let for safe access
                        MeasurementFields(
                            measurement = m, // Pass non-null measurement
                            onMeasurementChange = { measurement = it }
                        )
                    }
                }
                // Removed ClinicalDataFields call as file handling is now above
                 else -> {} // Handle other types if necessary (no specific fields for others currently)
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
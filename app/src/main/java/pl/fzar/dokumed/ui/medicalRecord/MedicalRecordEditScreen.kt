package pl.fzar.dokumed.ui.medicalRecord

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.Measurement
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.clinicalDataRecords
import pl.fzar.dokumed.data.model.consultationRecords
import pl.fzar.dokumed.data.model.getLocalizedString
import pl.fzar.dokumed.data.model.measurementRecords
import pl.fzar.dokumed.ui.components.ConfirmationDialog
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

    val isNewRecord = medicalRecord == null
    val initialDate = medicalRecord?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
    val initialType = medicalRecord?.type ?: MedicalRecordType.CONSULTATION

    var date by remember { mutableStateOf(initialDate) }
    var type by remember { mutableStateOf(initialType) }
    var expandedTypeSelector by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(medicalRecord?.description ?: "") }
    var doctor by remember { mutableStateOf(medicalRecord?.doctor ?: "") }
    var notes by remember { mutableStateOf(medicalRecord?.notes ?: "") }
    var tags by remember { mutableStateOf(medicalRecord?.tags?.toSet() ?: emptySet()) }
    var newTag by remember { mutableStateOf("") }

    var clinicalDataList by remember {
        mutableStateOf(medicalRecord?.clinicalData ?: emptyList())
    }

    var measurement by remember {
        mutableStateOf(medicalRecord?.measurements?.firstOrNull() ?: Measurement())
    }

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
        if (uri == null) return@rememberLauncherForActivityResult

        var fileName = "uploaded_file_${Uuid.random()}"
        val mimeType = context.contentResolver.getType(uri)
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
        copyFileToLocalStorage(context, uri, fileName) { savedFilePath ->
            val newClinicalData = ClinicalData(
                filePath = savedFilePath,
                fileMimeType = mimeType,
                fileName = fileName // Store the original filename
            )
            clinicalDataList = clinicalDataList + newClinicalData
        }
    }

    // Handle pending attachment for new records
    LaunchedEffect(isNewRecord, pendingAttachment) {
        if (isNewRecord && pendingAttachment != null) {
            if (clinicalDataList.none { it.filePath == pendingAttachment.filePath }) {
                clinicalDataList = clinicalDataList + pendingAttachment
            }
            consumesPendingAttachment()// Consume it after adding
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_record)) }, 
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                                contentDescription = stringResource(R.string.delete_record),
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
                    val currentMeasurement = measurement

                    val measurementsList = if (type in measurementRecords) {
                        listOf(currentMeasurement.copy(value = currentMeasurement.value?.toString()?.toDoubleOrNull()))
                    } else {
                        emptyList()
                    }

                    val finalClinicalDataList = if (type in clinicalDataRecords || type in consultationRecords) {
                        clinicalDataList
                    } else {
                        emptyList()
                    }

                    val updatedRecord = if (isNewRecord) {
                        MedicalRecord(
                            id = Uuid.random(),
                            date = date,
                            type = type,
                            description = description,
                            notes = notes,
                            tags = tags.toList(),
                            measurements = measurementsList,
                            clinicalData = finalClinicalDataList,
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
                            clinicalData = finalClinicalDataList,
                            doctor = doctor
                        )
                    }

                    onRecordEdited(updatedRecord)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = stringResource(R.string.save),
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
            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text(stringResource(R.string.date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.select_date))
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedTypeSelector,
                onExpandedChange = { expandedTypeSelector = !expandedTypeSelector },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = type.getLocalizedString(context),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.type)) },
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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text(stringResource(R.string.doctor)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text(stringResource(R.string.add_tag)) },
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
                                contentDescription = stringResource(R.string.remove_tag),
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
                    AttachedFilesSection(
                        clinicalDataList = clinicalDataList,
                        onAddFileClick = { filePickerLauncher.launch("*/*") },
                        onRemoveFileClick = { fileData ->
                            clinicalDataList = clinicalDataList.filter { it.id != fileData.id && it.filePath != fileData.filePath }
                            // Optional: Delete the actual file from storage here if needed
                        }
                    )
                }
                in measurementRecords -> {
                    measurement.let { m -> // Use let for safe access
                        MeasurementFields(
                            measurement = m, // Pass non-null measurement
                            onMeasurementChange = { measurement = it }
                        )
                    }
                }
                 else -> {} // Handle other types if necessary (no specific fields for others currently)
            }

            // Add some extra space at the bottom to ensure content isn't hidden behind the FAB
            Spacer(Modifier.height(80.dp))
            
            ConfirmationDialog(
                showDialog = showDeleteConfirmation,
                onDismissRequest = { showDeleteConfirmation = false },
                onConfirm = {
                    onDeleteRecord?.invoke(medicalRecord!!)
                    onBackClick()
                },
                title = stringResource(R.string.deletion_confirmation_title),
                text = stringResource(R.string.deletion_confirmation_message)
            )
        }
    }
}
package pl.fzar.dokumed.ui.medicalRecord

import android.net.Uri
import android.app.DatePickerDialog
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.getLocalizedString
import pl.fzar.dokumed.ui.theme.DokumedTheme
import java.util.Calendar
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationRecordEditScreen(
    consultationRecord: ConsultationRecord,
    onBackClick: () -> Unit,
    onRecordEdited: (ConsultationRecord) -> Unit,
    copyFileToLocalStorage: (Context, Uri, String) -> Unit,
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(consultationRecord.date) }
    var type by remember { mutableStateOf(consultationRecord.type) }
    var description by remember { mutableStateOf(consultationRecord.description ?: "") }
    var notes by remember { mutableStateOf(consultationRecord.notes ?: "") }
    var tags by remember { mutableStateOf(consultationRecord.tags) }
    var newTag by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf(consultationRecord.doctor ?: "") }
    var filePath by remember { mutableStateOf(consultationRecord.filePath) }
    var fileMimeType by remember { mutableStateOf(consultationRecord.fileMimeType) }
    var expandedTypeSelector by remember { mutableStateOf(false) }

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

            filePath = fileName
            fileMimeType = context.contentResolver.getType(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edytuj konsultację") },
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

            ExposedDropdownMenuBox(
                expanded = expandedTypeSelector,
                onExpandedChange = { expandedTypeSelector = !expandedTypeSelector },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = type.getLocalizedString(LocalContext.current),
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

            // Lekarz
            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("Lekarz") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dodawanie tagów
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

            // Wyświetlanie tagów
            Text("Tagi:", style = MaterialTheme.typography.bodyMedium)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = { tags = tags.filter { it != tag } },
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
            Spacer(modifier = Modifier.height(16.dp))

            // Wgrywanie pliku
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Wgraj plik")
            }

            filePath?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wybrano plik: ${Uri.parse(it).lastPathSegment ?: "Nieznany"}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Zapisz zmiany
            Button(
                onClick = {
                    val updatedRecord = consultationRecord.copy(
                        date = date,
                        type = type,
                        description = description,
                        notes = notes,
                        tags = tags,
                        doctor = doctor,
                        filePath = filePath,
                        fileMimeType = fileMimeType
                    )
                    onRecordEdited(updatedRecord)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz zmiany")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsultationRecordEditScreenPreview() {
    DokumedTheme {
        ConsultationRecordEditScreen(
            consultationRecord = ConsultationRecord(
                id = Uuid.random(),
                date = LocalDate(2025, 4, 26),
                type = MedicalRecordType.CONSULTATION,
                description = "Opis przykładowej konsultacji",
                notes = "Notatki lekarskie",
                tags = listOf("neurolog", "kontrola"),
                doctor = "Dr. Anna Nowak",
                filePath = null,
                fileMimeType = null
            ),
            onBackClick = {},
            onRecordEdited = {},
            copyFileToLocalStorage = { _, _, _ -> "" }
        )
    }
}
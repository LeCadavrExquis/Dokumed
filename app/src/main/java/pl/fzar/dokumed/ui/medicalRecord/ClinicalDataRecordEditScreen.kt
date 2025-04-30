package pl.fzar.dokumed.ui.medicalRecord

import android.net.Uri
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
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalDataRecordEditScreen(
    clinicalDataRecord: ClinicalDataRecord,
    onBackClick: () -> Unit,
    onRecordEdited: (ClinicalDataRecord) -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(clinicalDataRecord.date) }
    var description by remember { mutableStateOf(clinicalDataRecord.description ?: "") }
    var notes by remember { mutableStateOf(clinicalDataRecord.notes ?: "") }
    var tags by remember { mutableStateOf(clinicalDataRecord.tags) }
    var newTag by remember { mutableStateOf("") }
    var testName by remember { mutableStateOf(clinicalDataRecord.testName) }
    var filePath by remember { mutableStateOf(clinicalDataRecord.filePath) }
    var fileMimeType by remember { mutableStateOf(clinicalDataRecord.fileMimeType) }

    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = LocalDate(year, month + 1, dayOfMonth)
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
    }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            filePath = it.toString()
            fileMimeType = context.contentResolver.getType(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edytuj badanie kliniczne") },
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

            // Nazwa testu
            OutlinedTextField(
                value = testName,
                onValueChange = { testName = it },
                label = { Text("Nazwa testu") },
                modifier = Modifier.fillMaxWidth()
            )
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
            Spacer(modifier = Modifier.height(16.dp))

            // Upload pliku
            Text("Plik załączony:", style = MaterialTheme.typography.bodyMedium)
            if (filePath != null) {
                Text(
                    text = filePath ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Typ MIME: ${fileMimeType ?: "Nieznany"}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Brak pliku", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wybierz plik")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Zapisz zmiany
            Button(
                onClick = {
                    val updatedRecord = clinicalDataRecord.copy(
                        date = date,
                        description = description,
                        notes = notes,
                        tags = tags,
                        testName = testName,
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
fun PreviewClinicalDataRecordEditScreen() {
    ClinicalDataRecordEditScreen(
        clinicalDataRecord = ClinicalDataRecord(
            date = LocalDate(2025, 4, 26),
            type = pl.fzar.dokumed.data.model.MedicalRecordType.LAB_TEST,
            description = "Opis badania",
            notes = "Notatki",
            tags = listOf("Krew", "Test"),
            testName = "Morfologia",
            filePath = null,
            fileMimeType = null
        ),
        onBackClick = {},
        onRecordEdited = {}
    )
}
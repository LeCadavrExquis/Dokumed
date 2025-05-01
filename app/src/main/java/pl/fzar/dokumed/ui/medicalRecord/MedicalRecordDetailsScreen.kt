package pl.fzar.dokumed.ui.medicalRecord

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.clinicalDataRecords
import pl.fzar.dokumed.data.model.consultationRecords
import pl.fzar.dokumed.data.model.dummyRecords
import pl.fzar.dokumed.data.model.getLocalizedString
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordDetailsScreen(
    recordId: String?,
    onNavigateBack: () -> Unit,
    onEditRecord: (String) -> Unit,
    medicalRecord: MedicalRecord?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły Rekordu") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    if (recordId != null) {
                        IconButton(onClick = { onEditRecord(recordId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edytuj")
                        }
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
            if (medicalRecord != null) {
                val context = LocalContext.current
                Text(
                    text = medicalRecord.type.getLocalizedString(context),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data: ${medicalRecord.date}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!medicalRecord.description.isNullOrEmpty()) {
                    Text(
                        text = "Opis: ${medicalRecord.description}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (!medicalRecord.notes.isNullOrEmpty()) {
                    Text(
                        text = "Notatki: ${medicalRecord.notes}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Display Tags
                if (medicalRecord.tags.isNotEmpty()) {
                    Text(
                        text = "Tagi:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        medicalRecord.tags.forEach { tag ->
                            AssistChip(
                                modifier = Modifier.padding(end = 8.dp),
                                onClick = { /* Akcja po kliknięciu tagu */ },
                                label = { Text(tag) },
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Display Clinical Data (Attached Files) if available and relevant type
                if ((medicalRecord.type in consultationRecords || medicalRecord.type in clinicalDataRecords) && medicalRecord.clinicalData.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Załączone pliki", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Column { // Use Column for the list of files
                        medicalRecord.clinicalData.forEach { fileData ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { // Make the row clickable
                                        if (fileData.filePath != null && fileData.fileMimeType != null) {
                                            openFileIntent(context, fileData.filePath, fileData.fileMimeType)
                                        }
                                        // TODO: Add error handling if path or mime type is null
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // You might want an icon based on fileData.fileMimeType here later
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_file), // Generic file icon
                                    contentDescription = "Plik",
                                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                )
                                Text(
                                    text = fileData.fileName ?: fileData.filePath?.substringAfterLast('/') ?: "Nieznany plik",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                // Optional: Add an onClick to open the file later
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp)) // Add space after the file list
                }
            } else {
                Text("Nie można załadować szczegółów rekordu.")
            }
        }
    }
}

@Composable
fun OpenFileButton(filePath: String, mimeType: String, context: Context) {
    // Sprawdzamy, czy MIME typ jest wspierany przez urządzenie
    val mime = mimeType.ifEmpty { "application/octet-stream" } // domyślny MIME typ
    val fileUri = "file://$filePath".toUri()
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        IconButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { openFileIntent(context, filePath, mime) }
        ) {
            Text(text = "Otwórz plik")
        }
    }
}

fun openFileIntent(context: Context, filePath: String, mimeType: String) {
    // val uri = "file://$filePath".toUri() // Old way - causes FileUriExposedException
    val file = java.io.File(filePath)
    val authority = "${context.packageName}.provider" // Matches authority in Manifest
    val uri = FileProvider.getUriForFile(context, authority, file)

    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, mimeType)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        // Obsłuż błąd, np. wyświetl komunikat o braku aplikacji
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMedicalRecordDetailsScreen() {
    MedicalRecordDetailsScreen(
        medicalRecord = dummyRecords[8],
        recordId = Uuid.random().toString(),
        onNavigateBack = { },
        onEditRecord = { }
    )
}
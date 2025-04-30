package pl.fzar.dokumed.ui.medicalRecord

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
import pl.fzar.dokumed.data.model.MedicalRecord
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

                // Wyświetlanie szczegółów w zależności od typu rekordu
                when {
                    medicalRecord is ConsultationRecord -> {
                        Text(
                            text = "Szczegóły Konsultacji:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Lekarz: ${medicalRecord.doctor ?: "Brak"}")
                        medicalRecord.filePath?.let {
                            OpenFileButton(filePath = it, mimeType = medicalRecord.fileMimeType!!, context = context)
                        }
                    }
                    medicalRecord is ClinicalDataRecord -> {
                        Text(
                            text = "Szczegóły Badania Klinicznego:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Nazwa testu: ${medicalRecord.testName}")
                        medicalRecord.filePath?.let {
                            OpenFileButton(filePath = it, mimeType = medicalRecord.fileMimeType!!, context = context)
                        }
                    }
                    medicalRecord is MeasurementRecord -> {
                        Text(
                            text = "Szczegóły Pomiaru:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Nazwa testu: ${medicalRecord.testName}")
                        if (medicalRecord.value != null) {
                            Text("Wartość: ${medicalRecord.value} ${medicalRecord.unit ?: ""}")
                        } else {
                            Text("Wartość: Brak")
                        }
                    }
                    else -> {
                        Text("Brak dodatkowych szczegółów dla tego typu rekordu.")
                    }
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
    val uri = "file://$filePath".toUri()
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
package pl.fzar.dokumed.ui.medicalRecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.fzar.dokumed.data.model.ClinicalData

@Composable
fun AttachedFilesSection(
    clinicalDataList: List<ClinicalData>,
    onAddFileClick: () -> Unit,
    onRemoveFileClick: (ClinicalData) -> Unit
) {
    // Section for managing multiple files
    Text("Załączone pliki:", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    // List of attached files
    if (clinicalDataList.isNotEmpty()) {
        // Use Column as the list is expected to be short
        Column {
            clinicalDataList.forEach { fileData -> // Iterate directly
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = fileData.fileName ?: fileData.filePath?.substringAfterLast('/')
                        ?: "Nieznany plik",
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = { onRemoveFileClick(fileData) },
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
    Button(onClick = onAddFileClick) {
        Icon(Icons.Filled.Add, contentDescription = "Dodaj plik", modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Dodaj plik")
    }
    Spacer(Modifier.height(16.dp)) // Add space after file section
}
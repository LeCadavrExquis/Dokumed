package pl.fzar.dokumed.ui.medicalRecord

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.clinicalDataRecords

@Composable
fun ClinicalDataFields(
    clinicalData: ClinicalData, // Expect non-null
    recordType: MedicalRecordType,
    onClinicalDataChange: (ClinicalData) -> Unit,
    onFilePick: () -> Unit
) {
    // File attachment (Common for Consultation, Lab Test, Imaging, Procedure)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onFilePick) {
            Text("Załącz plik")
        }
        Spacer(Modifier.width(8.dp))
        clinicalData.filePath?.let {
            // Extract filename from path for display
            val fileName = it.substringAfterLast('/')
            Text(fileName.takeLast(30), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(8.dp))
}
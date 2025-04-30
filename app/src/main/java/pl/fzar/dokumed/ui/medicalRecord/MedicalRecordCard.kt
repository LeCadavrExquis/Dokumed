package pl.fzar.dokumed.ui.medicalRecord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.dummyRecords
import pl.fzar.dokumed.data.model.getLocalizedString

@Composable
fun MedicalRecordCard(
    record: MedicalRecord,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = record.date.toString(), style = MaterialTheme.typography.labelSmall)
                Text(text = record.type.getLocalizedString(context), style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = record.description ?: "", style = MaterialTheme.typography.bodyLarge)
            if (record.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                ) {
                    record.tags.forEach { tag ->
                        AssistChip(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = { },
                            label = { Text(tag) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun MedicalRecordCardPreview() {
    MedicalRecordCard(
        record = dummyRecords[7]
    )
}
package pl.fzar.dokumed.ui.export

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.MedicalRecord
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    records: List<MedicalRecord>,
    onBack: () -> Unit,
    exportRecords: (Set<Uuid>, dest: Uri) -> Unit,
    exportState: ExportState,
) {
    val selectedRecordIds = remember { mutableStateMapOf<Uuid, Boolean>() }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let { it ->
                val selectedIds = selectedRecordIds.filter { id -> id.value }.keys
                if (selectedIds.isNotEmpty()) {
                    exportRecords(selectedIds, it)
                } else {
                    Toast.makeText(context, "No records selected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    LaunchedEffect(records) {
        records.forEach { record ->
            if (!selectedRecordIds.containsKey(record.id)) {
                selectedRecordIds[record.id] = false
            }
        }
        val currentRecordIds = records.map { it.id }.toSet()
        selectedRecordIds.keys.retainAll { it in currentRecordIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TopAppBar(
            title = { Text("Export Data") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Select records to export:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(records, key = { it.id }) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedRecordIds[record.id] ?: false,
                            onCheckedChange = { isChecked ->
                                selectedRecordIds[record.id] = isChecked
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("${record.date}: ${record.type.name}")
                            Text("${record.description?.take(50)}...")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    createDocumentLauncher.launch("dokumed_export_${System.currentTimeMillis()}.zip")
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = selectedRecordIds.any { it.value } && exportState !is ExportState.InProgress
            ) {
                if (exportState is ExportState.InProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Export Selected")
                }
            }

            when (exportState) {
                is ExportState.Success -> {
                    Text("Export successful!", color = MaterialTheme.colorScheme.primary)
                }
                is ExportState.Error -> {
                    Text(exportState.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }
}

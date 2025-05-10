package pl.fzar.dokumed.ui.export

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.ui.components.FilterBottomSheet
import kotlin.uuid.Uuid

@RequiresApi(Build.VERSION_CODES.O) // Needed for FilterBottomSheet date pickers
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    // Observe states from ViewModel
    filteredRecords: List<MedicalRecord>,
    allTags: List<String>,
    selectedTypes: Set<MedicalRecordType>,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    selectedTags: Set<String>,
    exportState: ExportState,
    // Functions from ViewModel
    onBack: () -> Unit,
    exportRecords: (selectedIds: Set<Uuid>, dest: Uri, sendEmail: Boolean) -> Unit,
    updateSelectedTypes: (Set<MedicalRecordType>) -> Unit, // Renamed from onTypesChange
    updateDateRange: (LocalDate?, LocalDate?) -> Unit, // Renamed from onDateRangeChange
    updateSelectedTags: (Set<String>) -> Unit // Renamed from onTagsChange
) {
    val selectedRecordIds = remember { mutableStateMapOf<Uuid, Boolean>() }
    var sendEmailChecked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Filter sheet state remains local UI state
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Adjust selection state when filters change (observing ViewModel's filteredRecords)
    LaunchedEffect(filteredRecords) {
        val filteredIds = filteredRecords.map { it.id }.toSet()
        val currentSelectedIds = selectedRecordIds.filter { it.value }.keys
        val deselectedIds = currentSelectedIds - filteredIds
        deselectedIds.forEach { selectedRecordIds[it] = false }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let { destUri ->
                // Export only selected IDs that are also in the filtered list
                val filteredRecordIdsSet = filteredRecords.map { fr -> fr.id }.toSet()
                val finalSelectedIds = selectedRecordIds.filter { entry -> entry.value && entry.key in filteredRecordIdsSet }.keys

                if (finalSelectedIds.isNotEmpty()) {
                    exportRecords(finalSelectedIds, destUri, sendEmailChecked)
                } else {
                    Toast.makeText(context, "No records selected matching the current filter", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success && exportState.sendEmail) {
            sendEmailWithAttachment(context, exportState.zipUri)
            // Consider resetting state here or in ViewModel
        }
        // Removed onResetExportState as it's not in the new signature
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
            },
            actions = {
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(painterResource(R.drawable.ic_filter_list), contentDescription = "Filter Records")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Select records to export:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (filteredRecords.isEmpty()) {
                    Text(
                        "No records match the current filter.",
                        modifier = Modifier.weight(1f).align(Alignment.CenterHorizontally).padding(top = 20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredRecords, key = { it.id }) { record ->
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
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        createDocumentLauncher.launch("dokumed_export_${System.currentTimeMillis()}.zip")
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = selectedRecordIds.any { entry -> entry.value && filteredRecords.any { fr -> fr.id == entry.key } } && exportState !is ExportState.InProgress
                ) {
                    if (exportState is ExportState.InProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Export Selected")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                ) {
                    Checkbox(
                        checked = sendEmailChecked,
                        onCheckedChange = { sendEmailChecked = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Send via Email after export")
                }

                when (exportState) {
                    is ExportState.Success -> {
                        val message = if (exportState.sendEmail) "Export successful! Sending email..." else "Export successful!"
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    is ExportState.Error -> {
                        Text(
                            exportState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {}
                }
            }

            // Filter Bottom Sheet
            if (showFilterSheet) {
                ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
                    FilterBottomSheet(
                        allTypes = MedicalRecordType.entries.toList(),
                        selectedTypes = selectedTypes, // From ViewModel
                        onTypesChange = updateSelectedTypes, // To ViewModel
                        dateFrom = dateFrom, // From ViewModel
                        dateTo = dateTo, // From ViewModel
                        onDateRangeChange = updateDateRange, // To ViewModel
                        allTags = allTags, // From ViewModel
                        selectedTags = selectedTags, // From ViewModel
                        onTagsChange = updateSelectedTags, // To ViewModel
                        onDismiss = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showFilterSheet = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun sendEmailWithAttachment(context: android.content.Context, zipUri: Uri) {
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_SUBJECT, "Dokumed Export")
        putExtra(Intent.EXTRA_TEXT, "Please find the exported Dokumed data attached.")
        putExtra(Intent.EXTRA_STREAM, zipUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not start email app.", Toast.LENGTH_SHORT).show()
    }
}

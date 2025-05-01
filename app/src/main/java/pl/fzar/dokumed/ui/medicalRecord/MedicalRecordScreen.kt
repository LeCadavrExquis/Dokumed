import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.dummyRecords
import pl.fzar.dokumed.ui.components.FilterBottomSheet
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordCard

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordsScreen(
    records: List<MedicalRecord>,
    allTags: List<String>,
    onRecordClick: (MedicalRecord) -> Unit,
    onAddRecordClick: () -> Unit,
    // Filters state hosted in ViewModel
    selectedTypes: Set<MedicalRecordType>,
    onTypesChange: (Set<MedicalRecordType>) -> Unit,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    onDateRangeChange: (from: LocalDate?, to: LocalDate?) -> Unit,
    selectedTags: Set<String>,
    onTagsChange: (Set<String>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars) // Add this line for edge-to-edge
    ) {
        // TopAppBar without being in Scaffold
        TopAppBar(
            title = {
                Text("Dokumentacja Medyczna")
            },
            actions = {
                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                    Icon(Icons.Filled.Search, contentDescription = "Szukaj")
                }
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_list),
                        contentDescription = "Filtruj"
                    )
                }
            }
        )

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showSearchBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var localSearchText by remember { mutableStateOf(searchText) }
                        TextField(
                            value = localSearchText,
                            onValueChange = {
                                localSearchText = it
                                searchText = it
                            },
                            placeholder = { Text("Szukaj po opisie...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 0.dp) // No icon, so no extra padding needed
                        )
                    }
                }

                // Filter bottom sheet
                if (showFilterSheet) {
                    ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
                        FilterBottomSheet(
                            allTypes = MedicalRecordType.values().toList(),
                            selectedTypes = selectedTypes,
                            onTypesChange = onTypesChange,
                            dateFrom = dateFrom,
                            dateTo = dateTo,
                            onDateRangeChange = onDateRangeChange,
                            allTags = allTags,
                            selectedTags = selectedTags,
                            onTagsChange = onTagsChange,
                            onDismiss = { showFilterSheet = false }
                        )
                    }                }
                
                // Records list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(records.filter { rec ->
                        // apply search and filters
                        val matchesSearch = searchText.isBlank() || (rec.description?.contains(searchText, ignoreCase = true) == true)
                        val matchesType = selectedTypes.isEmpty() || selectedTypes.contains(rec.type)
                        val matchesDate = (dateFrom == null || rec.date >= dateFrom) &&
                                (dateTo == null || rec.date <= dateTo)
                        val matchesTags = selectedTags.isEmpty() || rec.tags.any { selectedTags.contains(it) }
                        matchesSearch && matchesType && matchesDate && matchesTags
                    }) { record ->
                        MedicalRecordCard(record = record, onClick = { onRecordClick(record) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // FloatingActionButton positioned at bottom-end
            FloatingActionButton(
                onClick = onAddRecordClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj rekord")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PreviewMedicalRecordsScreen() {
    MedicalRecordsScreen(
        records = dummyRecords,
        allTags = listOf("tag1", "tag2", "kontrola"),
        onRecordClick = {},
        onAddRecordClick = {},
        selectedTypes = setOf(),
        onTypesChange = {},
        dateFrom = null,
        dateTo = null,
        onDateRangeChange = { _, _ -> },
        selectedTags = setOf(),
        onTagsChange = {}
    )
}


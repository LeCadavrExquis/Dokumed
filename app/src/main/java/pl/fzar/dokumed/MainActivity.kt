package pl.fzar.dokumed

import MedicalRecordsScreen
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordDetailsScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordEditScreen
import pl.fzar.dokumed.ui.statistics.StatisticsScreen
import pl.fzar.dokumed.ui.statistics.StatisticsViewModel
import pl.fzar.dokumed.ui.theme.DokumedTheme
import pl.fzar.dokumed.ui.components.AppBottomNavigationBar
import pl.fzar.dokumed.ui.export.ExportScreen
import pl.fzar.dokumed.ui.export.ExportViewModel
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordViewModel
import kotlin.uuid.Uuid

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val medVM: MedicalRecordViewModel = org.koin.androidx.compose.koinViewModel()
            val statsVM: StatisticsViewModel = org.koin.androidx.compose.koinViewModel()
            val exportVM: ExportViewModel = org.koin.androidx.compose.koinViewModel()
            DokumedTheme {
                Scaffold(
                    bottomBar = {
                        // Only show bottom bar on main screens, not on detail or edit screens
                        val currentRoute =
                            navController.currentBackStackEntryAsState().value?.destination?.route
                        val mainRoutes = listOf("records", "statistics", "export")

                        if (currentRoute in mainRoutes) {
                            AppBottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "records",
                        modifier = Modifier.padding(
                            PaddingValues(
                                bottom = innerPadding.calculateBottomPadding()
                            )
                        )
                    ) {

                        composable("records") {
                            val records by medVM.filteredRecords.collectAsState()

                            val selectedTypes by medVM.selectedTypes.collectAsState()
                            val selectedTags by medVM.selectedTags.collectAsState()
                            val dateFrom by medVM.dateFrom.collectAsState()
                            val dateTo by medVM.dateTo.collectAsState()
                            val allTags by medVM.availableTags.collectAsState()
                            MedicalRecordsScreen(
                                records = records,
                                allTags = allTags,
                                onRecordClick = { record ->
                                    navController.navigate("record_details/${record.id}")
                                },
                                onAddRecordClick = { navController.navigate("record_new") },
                                selectedTypes = selectedTypes,
                                onTypesChange = { medVM.updateSelectedTypes(it) },
                                dateFrom = dateFrom,
                                dateTo = dateTo,
                                onDateRangeChange = { from, to ->
                                    medVM.updateDateFrom(from); medVM.updateDateTo(
                                    to
                                )
                                },
                                selectedTags = selectedTags,
                                onTagsChange = { medVM.updateSelectedTags(it) }
                            )
                        }
                        composable("record_details/{recordId}") { backStackEntry ->
                            val recordIdString = backStackEntry.arguments?.getString("recordId")
                            val recordId = recordIdString?.let {
                                try { Uuid.parse(it) } catch (e: IllegalArgumentException) { null }
                            }
                            val currentRecord by medVM.currentRecord.collectAsState()

                            // Use LaunchedEffect to load details when recordId is valid
                            LaunchedEffect(recordId) {
                                if (recordId != null) {
                                    // Assuming loadRecordDetailsById exists in VM
                                    medVM.loadRecordDetailsById(recordId)
                                } else {
                                    // Handle invalid ID if necessary, maybe clear currentRecord
                                    medVM.clearCurrentRecord() // Add this function to VM
                                }
                            }

                            if (recordId == null) {
                                Text("Nieprawidłowy ID rekordu") // Handle invalid ID from route
                            } else if (currentRecord != null && currentRecord?.id == recordId) {
                                // Ensure the loaded record matches the requested ID
                                MedicalRecordDetailsScreen(
                                    medicalRecord = currentRecord!!, // Not null here due to check
                                    recordId = recordId.toString(), // Pass the validated ID string
                                    onNavigateBack = { navController.popBackStack() },
                                    onEditRecord = { navController.navigate("record_edit/${currentRecord!!.id}") },
                                )
                            } else {
                                // Show loading indicator while currentRecord is null or doesn't match
                                CircularProgressIndicator()
                                // Optionally add Text("Ładowanie...")
                            }
                        }
                        composable("record_edit/{recordId}") { backStackEntry ->
                            val recordIdString = backStackEntry.arguments?.getString("recordId")
                             val recordId = recordIdString?.let {
                                try { Uuid.parse(it) } catch (e: IllegalArgumentException) { null }
                            }
                            val currentRecord by medVM.currentRecord.collectAsState()

                            // Ensure correct record is loaded for editing
                            LaunchedEffect(recordId) {
                                if (recordId != null && currentRecord?.id != recordId) {
                                     medVM.loadRecordDetailsById(recordId) // Ensure correct record is loaded
                                } else if (recordId == null) {
                                     medVM.clearCurrentRecord() // Add this function to VM
                                }
                            }


                            if (recordId == null) {
                                Text("Nieprawidłowy ID rekordu do edycji")
                            } else if (currentRecord != null && currentRecord?.id == recordId) {
                                MedicalRecordEditScreen(
                                    medicalRecord = currentRecord!!,
                                    onBackClick = { navController.popBackStack() },
                                    onRecordEdited = { updatedRecord ->
                                        medVM.updateRecord(updatedRecord)
                                        navController.popBackStack()
                                    },
                                    copyFileToLocalStorage = medVM::copyFileToLocalStorage,
                                    onDeleteRecord = { record ->
                                        medVM.deleteRecord(record)
                                        navController.popBackStack() // Navigate back after delete
                                    }
                                )
                            } else {
                                // Loading indicator for edit screen
                                CircularProgressIndicator()
                                // Optionally add Text("Ładowanie edycji...")
                            }
                        }
                        composable("record_new") {
                            // Create a new record screen with null record
                            MedicalRecordEditScreen(
                                medicalRecord = null,
                                onBackClick = { navController.popBackStack() },
                                onRecordEdited = { newRecordData ->
                                    medVM.addNewRecord(newRecordData)
                                    navController.popBackStack()
                                },
                                copyFileToLocalStorage = medVM::copyFileToLocalStorage,
                                // No delete option for new records
                                onDeleteRecord = null
                            )
                        }
                        composable("statistics") {
                            val chartData by statsVM.chartData.collectAsState()
                            val selectedType by statsVM.selectedType.collectAsState()
                            val metric by statsVM.metric.collectAsState()
                            val dateFrom by statsVM.dateFrom.collectAsState()
                            val dateTo by statsVM.dateTo.collectAsState()

                            StatisticsScreen(
                                selectedType = selectedType,
                                selectType = { statsVM.selectType(it) },
                                metric = metric,
                                selectMetric = { statsVM.selectMetric(it) },
                                dateFrom = dateFrom,
                                dateTo = dateTo,
                                chartData = chartData,
                                updateDateRange = { from, to -> statsVM.updateDateRange(from, to) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("export") {
                            val records by medVM.filteredRecords.collectAsState()
                            val exportState by exportVM.exportState.collectAsState()
                            ExportScreen(
                                records = records,
                                onBack = { navController.popBackStack() },
                                exportRecords = exportVM::exportRecords,
                                exportState = exportState
                            )
                        }
                    }
                }
            }
        }
    }
}
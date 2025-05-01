package pl.fzar.dokumed

import MedicalRecordsScreen
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext
import pl.fzar.dokumed.ui.components.AppBottomNavigationBar
import pl.fzar.dokumed.ui.components.BottomNavItem
import pl.fzar.dokumed.ui.export.ExportScreen
import pl.fzar.dokumed.ui.export.ExportViewModel
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordDetailsScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordEditScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordViewModel
import pl.fzar.dokumed.ui.statistics.StatisticsScreen
import pl.fzar.dokumed.ui.statistics.StatisticsViewModel
import pl.fzar.dokumed.ui.theme.DokumedTheme
import kotlin.uuid.Uuid

class MainActivity : ComponentActivity() {
    // State to trigger navigation after intent processing
    private var pendingNavigation by mutableStateOf<String?>(null)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle the intent that started the activity
        handleIntent(intent)

        setContent {
            // Get the ViewModel using Koin
            val medicalRecordViewModel: MedicalRecordViewModel = koinViewModel()

            DokumedTheme {
                val navController = rememberNavController()
                
                // Main layout with navigation
                Scaffold(
                    bottomBar = {
                        AppBottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.Records.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(BottomNavItem.Records.route) {
                            MedicalRecordsScreen(
                                records = medicalRecordViewModel.filteredRecords.collectAsState().value,
                                allTags = medicalRecordViewModel.allTags.collectAsState().value,
                                onRecordClick = { record ->
                                    navController.navigate("record_detail/${record.id}")
                                },
                                onAddRecordClick = {
                                    navController.navigate("record_new")
                                },
                                selectedTypes = medicalRecordViewModel.selectedTypes.collectAsState().value,
                                onTypesChange = { types ->
                                    medicalRecordViewModel.updateSelectedTypes(types)
                                },
                                dateFrom = medicalRecordViewModel.dateRangeStart.collectAsState().value,
                                dateTo = medicalRecordViewModel.dateRangeEnd.collectAsState().value,
                                onDateRangeChange = { start, end ->
                                    medicalRecordViewModel.updateDateRange(start, end)
                                },
                                selectedTags = medicalRecordViewModel.selectedTags.collectAsState().value,
                                onTagsChange = { tags ->
                                    medicalRecordViewModel.updateSelectedTags(tags)
                                }
                            )
                        }
                        composable(BottomNavItem.Statistics.route) {
                            // Use statistics ViewModel directly with koinViewModel
                            val statisticsViewModel: StatisticsViewModel = koinViewModel()
                            StatisticsScreen(
                                selectedType = statisticsViewModel.selectedType.collectAsState().value,
                                selectType = statisticsViewModel::selectType,
                                metric = statisticsViewModel.metric.collectAsState().value,
                                selectMetric = statisticsViewModel::selectMetric,
                                dateFrom = statisticsViewModel.dateFrom.collectAsState().value,
                                dateTo = statisticsViewModel.dateTo.collectAsState().value,
                                chartData = statisticsViewModel.chartData.collectAsState().value,
                                updateDateRange = statisticsViewModel::updateDateRange,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(BottomNavItem.Export.route) {
                            // Use export ViewModel directly with koinViewModel
                            val exportViewModel: ExportViewModel = koinViewModel()
                            ExportScreen(
                                records = medicalRecordViewModel.allRecords.collectAsState().value,
                                onBack = { navController.popBackStack() },
                                exportRecords = exportViewModel::exportRecords,
                                exportState = exportViewModel.exportState.collectAsState().value
                            )
                        }
                        composable("record_detail/{recordId}") { backStackEntry ->
                            val recordId = backStackEntry.arguments?.getString("recordId")
                            // When this screen is shown, load the record by ID
                            LaunchedEffect(recordId) {
                                if (recordId != null) {
                                    medicalRecordViewModel.loadRecordDetailsById(Uuid.parse(recordId))
                                }
                            }
                            MedicalRecordDetailsScreen(
                                recordId = recordId,
                                onNavigateBack = { navController.popBackStack() },
                                onEditRecord = { recordId ->
                                    navController.navigate("record_edit/$recordId")
                                },
                                medicalRecord = medicalRecordViewModel.currentRecord.collectAsState().value
                            )
                        }
                        composable("record_new") {
                            // Initialize view model for new record
                            LaunchedEffect(Unit) {
                                // Reset all the state for creating a new record
                                medicalRecordViewModel.loadMedicalRecord(null) 
                            }
                        
                            MedicalRecordEditScreen(
                                medicalRecord = null, // null for new record
                                onBackClick = { navController.popBackStack() },
                                onRecordEdited = { record ->
                                    // Call addNewRecord for new records
                                    medicalRecordViewModel.addNewRecord(record) 
                                    navController.popBackStack()
                                },
                                copyFileToLocalStorage = { context, uri, fileName, callback ->
                                    medicalRecordViewModel.copyFileToLocalStorage(
                                        context,
                                        uri,
                                        fileName,
                                        callback
                                    )
                                },
                                onDeleteRecord = null,
                                pendingAttachment = medicalRecordViewModel.pendingAttachment.collectAsState().value,
                                consumesPendingAttachment = medicalRecordViewModel::consumePendingAttachment
                            )
                        }
                        composable("record_edit/{recordId}") { backStackEntry ->
                            val recordId = backStackEntry.arguments?.getString("recordId")
                            // When editing an existing record, load it by ID
                            LaunchedEffect(recordId) {
                                if (recordId != null) {
                                    // Properly load record with all related data
                                    medicalRecordViewModel.loadMedicalRecord(Uuid.parse(recordId))
                                }
                            }
                            MedicalRecordEditScreen(
                                medicalRecord = medicalRecordViewModel.currentRecord.collectAsState().value,
                                onBackClick = { navController.popBackStack() },
                                onRecordEdited = { record ->
                                    // Call updateRecord for existing records
                                    medicalRecordViewModel.updateRecord(record)
                                    navController.popBackStack()
                                },
                                copyFileToLocalStorage = { context, uri, fileName, callback ->
                                    medicalRecordViewModel.copyFileToLocalStorage(
                                        context,
                                        uri,
                                        fileName,
                                        callback
                                    )
                                },
                                onDeleteRecord = { record ->
                                    medicalRecordViewModel.deleteRecord(record)
                                    navController.popBackStack()
                                },
                                pendingAttachment = null,
                                consumesPendingAttachment = {}
                            )
                        }
                    }
                }

                // Perform navigation after composition if pendingNavigation is set
                LaunchedEffect(pendingNavigation) {
                    pendingNavigation?.let { route ->
                        navController.navigate(route)
                        pendingNavigation = null // Reset after navigation
                    }
                }
            }
        }
    }

    // Handle intents received while the activity is running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Set the new intent for the activity
        setIntent(intent)
        // Handle the new intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri: Uri? = intent.data
            val mimeType: String? = intent.type ?: contentResolver.getType(uri!!) // Get MIME type

            if (uri != null && mimeType != null && (mimeType.startsWith("image/") || mimeType == "application/pdf")) {
                // Launch a coroutine to copy the file and update the ViewModel
                lifecycleScope.launch {
                    // Get the ViewModel through regular Koin DI
                    val medicalRecordViewModel = GlobalContext.get().get<MedicalRecordViewModel>()
                    val clinicalData = medicalRecordViewModel.copyFileToLocalStorage(this@MainActivity, uri, mimeType)
                    if (clinicalData != null) {
                        medicalRecordViewModel.setPendingAttachment(clinicalData)
                        // Set pending navigation to trigger after composition
                        pendingNavigation = "record_new" // Navigate to new record screen
                    }
                }
            }
        }
    }
}
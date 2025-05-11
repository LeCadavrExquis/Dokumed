package pl.fzar.dokumed

import MedicalRecordsScreen
import android.content.Context // Added for SharedPreferences
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext
import pl.fzar.dokumed.security.PinViewModel
import pl.fzar.dokumed.ui.components.AppBottomNavigationBar
import pl.fzar.dokumed.ui.components.BottomNavItem
import pl.fzar.dokumed.ui.export.ExportScreen
import pl.fzar.dokumed.ui.export.ExportViewModel
import pl.fzar.dokumed.ui.profile.ProfileScreen
import pl.fzar.dokumed.ui.profile.ProfileViewModel
import pl.fzar.dokumed.ui.profile.ProfileEditScreen // Added import
import pl.fzar.dokumed.ui.profile.MedicationReminderScreen // Added import
import pl.fzar.dokumed.ui.profile.CloudSyncScreen // Added import
import pl.fzar.dokumed.ui.profile.PanicButtonSettingsScreen // Added import
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordDetailsScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordEditScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordViewModel
import pl.fzar.dokumed.ui.onboarding.OnboardingScreen // Import OnboardingScreen
import pl.fzar.dokumed.ui.security.PinScreen
import pl.fzar.dokumed.ui.statistics.StatisticsScreen
import pl.fzar.dokumed.ui.statistics.StatisticsViewModel
import pl.fzar.dokumed.ui.theme.DokumedTheme
import kotlin.uuid.Uuid

class MainActivity : FragmentActivity() {
    // State to trigger navigation after intent processing
    private var pendingNavigation by mutableStateOf<String?>(null)
    private var isAuthenticated by mutableStateOf(false)
    private var isOnboardingCompleted by mutableStateOf(false) // Existing state, will be initialized from prefs

    // Initialize SharedPreferences
    private val prefs by lazy {
        getSharedPreferences("DokumedPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load onboarding completion state from SharedPreferences
        isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        // Handle the intent that started the activity
        handleIntent(intent)

        setContent {
            val pinViewModel: PinViewModel = koinViewModel()
            // Assuming PinViewModel exposes isPinEnabledFlow: StateFlow<Boolean>
            // This flow should reflect whether a PIN is configured and persisted.
            val isPinEnabled by pinViewModel.isPinEnabledFlow.collectAsState() // Added this line

            val medicalRecordViewModel: MedicalRecordViewModel = koinViewModel()
            val profileViewModel: ProfileViewModel = koinViewModel()

            DokumedTheme {
                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        pinViewModel = pinViewModel,
                        profileViewModel = profileViewModel,
                        onFinishOnboarding = {
                            // This callback is invoked after the user completes the onboarding flow,
                            // including making a decision about PIN setup (handled within OnboardingScreen & PinViewModel).
                            isOnboardingCompleted = true
                            // Save onboarding completion state to SharedPreferences
                            prefs.edit().putBoolean("onboarding_completed", true).apply()
                        }
                    )
                } else if (isPinEnabled && !isAuthenticated) { // Modified condition
                    // Show PIN screen only if a PIN is enabled and the user is not yet authenticated
                    PinScreen(
                        pinViewModel = pinViewModel,
                        activity = this as FragmentActivity,
                        onNavigateToMain = { isAuthenticated = true }
                    )
                } else {
                    // Onboarding is completed, and either PIN is not enabled or user is authenticated
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
                                // Collect necessary states from MedicalRecordViewModel
                                val records by medicalRecordViewModel.filteredRecords.collectAsState()
                                val allTags by medicalRecordViewModel.allTags.collectAsState()
                                val selectedTypes by medicalRecordViewModel.selectedTypes.collectAsState()
                                val dateFrom by medicalRecordViewModel.dateRangeStart.collectAsState()
                                val dateTo by medicalRecordViewModel.dateRangeEnd.collectAsState()
                                val selectedTags by medicalRecordViewModel.selectedTags.collectAsState()

                                MedicalRecordsScreen(
                                    records = records,
                                    allTags = allTags,
                                    onRecordClick = { record ->
                                        navController.navigate("record_detail/${record.id}")
                                    },
                                    onAddRecordClick = {
                                        navController.navigate("record_new")
                                    },
                                    selectedTypes = selectedTypes,
                                    onTypesChange = medicalRecordViewModel::updateSelectedTypes,
                                    dateFrom = dateFrom,
                                    dateTo = dateTo,
                                    onDateRangeChange = medicalRecordViewModel::updateDateRange, // Pass the correct function
                                    selectedTags = selectedTags,
                                    onTagsChange = medicalRecordViewModel::updateSelectedTags
                                )
                            }
                            composable(BottomNavItem.Statistics.route) {
                                // Use statistics ViewModel directly with koinViewModel
                                val statisticsViewModel: StatisticsViewModel = koinViewModel()
                                // Collect states from StatisticsViewModel
                                val selectedType by statisticsViewModel.selectedType.collectAsState()
                                val metric by statisticsViewModel.metric.collectAsState()
                                val dateFrom by statisticsViewModel.dateFrom.collectAsState()
                                val dateTo by statisticsViewModel.dateTo.collectAsState()
                                val chartData by statisticsViewModel.chartData.collectAsState()

                                StatisticsScreen(
                                    selectedType = selectedType,
                                    selectType = statisticsViewModel::selectType,
                                    metric = metric,
                                    selectMetric = statisticsViewModel::selectMetric,
                                    dateFrom = dateFrom,
                                    dateTo = dateTo,
                                    chartData = chartData,
                                    updateDateRange = statisticsViewModel::updateDateRange,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(BottomNavItem.Export.route) {
                                // Use export ViewModel directly with koinViewModel
                                val exportViewModel: ExportViewModel = koinViewModel()
                                // Collect necessary states from ExportViewModel
                                val filteredRecords by exportViewModel.filteredRecords.collectAsState()
                                val allTags by exportViewModel.allTags.collectAsState()
                                val selectedTypes by exportViewModel.selectedTypes.collectAsState()
                                val dateFrom by exportViewModel.dateRangeStart.collectAsState()
                                val dateTo by exportViewModel.dateRangeEnd.collectAsState()
                                val selectedTags by exportViewModel.selectedTags.collectAsState()
                                val exportState by exportViewModel.exportState.collectAsState()

                                ExportScreen(
                                    filteredRecords = filteredRecords,
                                    allTags = allTags,
                                    selectedTypes = selectedTypes,
                                    dateFrom = dateFrom,
                                    dateTo = dateTo,
                                    selectedTags = selectedTags,
                                    exportState = exportState,
                                    onBack = { navController.popBackStack() }, // Added onBack
                                    exportRecords = { selectedIds, dest, sendEmail -> // Changed from onExportAction
                                        exportViewModel.exportRecords(selectedIds, dest, sendEmail)
                                    },
                                    updateSelectedTypes = exportViewModel::updateSelectedTypes, // Changed from onTypesChange
                                    updateDateRange = exportViewModel::updateDateRange, // Changed from onDateRangeChange
                                    updateSelectedTags = exportViewModel::updateSelectedTags // Changed from onTagsChange
                                    // Removed onShareFile and onResetExportState
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
                                    onEditRecord = { recordIdValue -> // Renamed to avoid conflict
                                        navController.navigate("record_edit/$recordIdValue")
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
                            composable(BottomNavItem.Profile.route) {
                                ProfileScreen(navController, profileViewModel)
                            }
                            composable("profileEdit") { // Route for ProfileEditScreen
                                ProfileEditScreen(
                                    navController = navController,
                                    viewModel = profileViewModel
                                )
                            }
                            composable("medicationReminders") { // Route for MedicationReminderScreen
                                MedicationReminderScreen(
                                    navController = navController,
                                    viewModel = profileViewModel
                                )
                            }
                            composable("cloudSync") { // Route for CloudSyncScreen
                                CloudSyncScreen(
                                    navController = navController,
                                    viewModel = profileViewModel
                                )
                            }
                            composable("panicButtonSettings") { // Route for PanicButtonSettingsScreen
                                PanicButtonSettingsScreen(
                                    navController = navController,
                                    viewModel = profileViewModel
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
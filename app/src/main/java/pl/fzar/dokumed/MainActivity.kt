package pl.fzar.dokumed

import MedicalRecordsScreen
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pl.fzar.dokumed.navigation.Routes // Import new Routes
import pl.fzar.dokumed.security.PinViewModel
import pl.fzar.dokumed.ui.components.AppBottomNavigationBar
import pl.fzar.dokumed.ui.export.ExportScreen
import pl.fzar.dokumed.ui.export.ExportViewModel
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordDetailsScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordEditScreen
import pl.fzar.dokumed.ui.medicalRecord.MedicalRecordViewModel
import pl.fzar.dokumed.ui.onboarding.OnboardingScreen
import pl.fzar.dokumed.ui.profile.CloudSyncScreen
import pl.fzar.dokumed.ui.profile.MedicationReminderScreen
import pl.fzar.dokumed.ui.profile.PanicButtonSettingsScreen
import pl.fzar.dokumed.ui.profile.ProfileEditScreen
import pl.fzar.dokumed.ui.profile.ProfileViewModel
import pl.fzar.dokumed.ui.profile.SettingsScreen
import pl.fzar.dokumed.ui.security.PinScreen
import pl.fzar.dokumed.ui.statistics.StatisticsScreen
import pl.fzar.dokumed.ui.statistics.StatisticsViewModel
import pl.fzar.dokumed.ui.theme.DokumedTheme
import kotlin.uuid.Uuid

class MainActivity : FragmentActivity() {
    private var pendingIntentUri by mutableStateOf<Uri?>(null)
    private var pendingIntentMimeType by mutableStateOf<String?>(null)

    private val prefs by lazy {
        getSharedPreferences("DokumedPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        handleIntent(intent, isOnboardingCompleted)

        setContent {
            DokumedTheme {
                val navController = rememberNavController()
                val pinViewModel: PinViewModel = koinViewModel()
                val isPinEnabled by pinViewModel.isPinEnabledFlow.collectAsState()
                var isAuthenticated by remember { mutableStateOf(false) }

                val startDestination = when {
                    !isOnboardingCompleted -> Routes.ONBOARDING
                    isPinEnabled && !isAuthenticated -> Routes.PIN_AUTH
                    else -> Routes.MAIN_APP_CONTENT
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.ONBOARDING) {
                        val profileViewModel: ProfileViewModel = koinViewModel()
                        OnboardingScreen(
                            pinViewModel = pinViewModel,
                            profileViewModel = profileViewModel,
                            onFinishOnboarding = {
                                prefs.edit { putBoolean("onboarding_completed", true) }
                                if (pinViewModel.isPinEnabledFlow.value) {
                                    navController.navigate(Routes.PIN_AUTH) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Routes.MAIN_APP_CONTENT) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Routes.PIN_AUTH) {
                        PinScreen(
                            pinViewModel = pinViewModel,
                            activity = this@MainActivity,
                            onNavigateToMain = {
                                isAuthenticated = true
                                navController.navigate(Routes.MAIN_APP_CONTENT) {
                                    popUpTo(Routes.PIN_AUTH) { inclusive = true }
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.MAIN_APP_CONTENT) {
                        MainAppContent(
                            activity = this@MainActivity,
                            initialPendingUri = pendingIntentUri,
                            initialPendingMimeType = pendingIntentMimeType,
                            onPendingIntentHandled = {
                                pendingIntentUri = null
                                pendingIntentMimeType = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        handleIntent(intent, isOnboardingCompleted)
    }

    private fun handleIntent(intent: Intent?, isOnboardingCompleted: Boolean) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri: Uri? = intent.data
            val mimeType: String? = intent.type ?: contentResolver.getType(uri!!)

            if (uri != null && mimeType != null && (mimeType.startsWith("image/") || mimeType == "application/pdf")) {
                if (isOnboardingCompleted) {
                    pendingIntentUri = uri
                    pendingIntentMimeType = mimeType
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    activity: FragmentActivity,
    initialPendingUri: Uri?,
    initialPendingMimeType: String?,
    onPendingIntentHandled: () -> Unit
) {
    val navController = rememberNavController() // For BottomNavigation items
    val medicalRecordViewModel: MedicalRecordViewModel = koinViewModel()

    LaunchedEffect(initialPendingUri, initialPendingMimeType) {
        if (initialPendingUri != null && initialPendingMimeType != null) {
            activity.lifecycleScope.launch {
                val clinicalData = medicalRecordViewModel.copyFileToLocalStorage(
                    activity,
                    initialPendingUri,
                    initialPendingMimeType
                )
                if (clinicalData != null) {
                    medicalRecordViewModel.setPendingAttachment(clinicalData)
                    // Navigate to new record screen, ensuring Records screen is in backstack
                    navController.navigate(Routes.RECORD_NEW) {
                        popUpTo(Routes.RECORDS) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                onPendingIntentHandled()
            }
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController = navController) // Uses Routes internally now
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.RECORDS, // Default screen for main app
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.RECORDS) {
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
                        navController.navigate(Routes.recordDetail(record.id.toString()))
                    },
                    onAddRecordClick = {
                        navController.navigate(Routes.RECORD_NEW)
                    },
                    selectedTypes = selectedTypes,
                    onTypesChange = medicalRecordViewModel::updateSelectedTypes,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onDateRangeChange = medicalRecordViewModel::updateDateRange,
                    selectedTags = selectedTags,
                    onTagsChange = medicalRecordViewModel::updateSelectedTags
                )
            }
            composable(Routes.STATISTICS) {
                val statisticsViewModel: StatisticsViewModel = koinViewModel()
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
            composable(Routes.EXPORT) {
                val exportViewModel: ExportViewModel = koinViewModel()
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
                    onBack = { navController.popBackStack() },
                    exportRecords = { selectedIds, dest, sendEmail ->
                        exportViewModel.exportRecords(selectedIds, dest, sendEmail)
                    },
                    updateSelectedTypes = exportViewModel::updateSelectedTypes,
                    updateDateRange = exportViewModel::updateDateRange,
                    updateSelectedTags = exportViewModel::updateSelectedTags
                )
            }
            composable(Routes.RECORD_DETAIL_TEMPLATE) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId")
                LaunchedEffect(recordId) {
                    if (recordId != null) {
                        medicalRecordViewModel.loadRecordDetailsById(Uuid.parse(recordId))
                    }
                }
                MedicalRecordDetailsScreen(
                    recordId = recordId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditRecord = { recordIdValue ->
                        navController.navigate(Routes.recordEdit(recordIdValue))
                    },
                    medicalRecord = medicalRecordViewModel.currentRecord.collectAsState().value
                )
            }
            composable(Routes.RECORD_NEW) {
                LaunchedEffect(Unit) {
                    medicalRecordViewModel.loadMedicalRecord(null)
                }
                MedicalRecordEditScreen(
                    medicalRecord = null,
                    onBackClick = { navController.popBackStack() },
                    onRecordEdited = { record ->
                        medicalRecordViewModel.addNewRecord(record)
                        navController.popBackStack()
                    },
                    copyFileToLocalStorage = { context, uri, fileName, callback ->
                        medicalRecordViewModel.copyFileToLocalStorage(context, uri, fileName, callback)
                    },
                    onDeleteRecord = null,
                    pendingAttachment = medicalRecordViewModel.pendingAttachment.collectAsState().value,
                    consumesPendingAttachment = medicalRecordViewModel::consumePendingAttachment
                )
            }
            composable(Routes.RECORD_EDIT_TEMPLATE) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId")
                LaunchedEffect(recordId) {
                    if (recordId != null) {
                        medicalRecordViewModel.loadMedicalRecord(Uuid.parse(recordId))
                    }
                }
                MedicalRecordEditScreen(
                    medicalRecord = medicalRecordViewModel.currentRecord.collectAsState().value,
                    onBackClick = { navController.popBackStack() },
                    onRecordEdited = { record ->
                        medicalRecordViewModel.updateRecord(record)
                        navController.popBackStack()
                    },
                    copyFileToLocalStorage = { context, uri, fileName, callback ->
                        medicalRecordViewModel.copyFileToLocalStorage(context, uri, fileName, callback)
                    },
                    onDeleteRecord = { record ->
                        medicalRecordViewModel.deleteRecord(record)
                        navController.popBackStack()
                    },
                    pendingAttachment = null,
                    consumesPendingAttachment = {}
                )
            }
            composable(Routes.PROFILE_SETTINGS) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                SettingsScreen(navController, profileViewModel) // SettingsScreen will use Routes for its internal navigation
            }
            composable(Routes.PROFILE_EDIT) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                ProfileEditScreen(navController = navController, viewModel = profileViewModel)
            }
            composable(Routes.MEDICATION_REMINDERS) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                MedicationReminderScreen(navController = navController, viewModel = profileViewModel)
            }
            composable(Routes.CLOUD_SYNC) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                CloudSyncScreen(navController = navController, viewModel = profileViewModel)
            }
            composable(Routes.PANIC_BUTTON_SETTINGS) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                PanicButtonSettingsScreen(navController = navController, viewModel = profileViewModel)
            }
        }
    }
}
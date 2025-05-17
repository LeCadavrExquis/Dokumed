package pl.fzar.dokumed.widgets.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences // Added import
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.state.getAppWidgetState // Added import
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.repository.ProfileRepository
import pl.fzar.dokumed.widgets.glance.theme.DokumedGlanceTheme
import pl.fzar.dokumed.ui.profile.ProfileScreenState

class ProfileGlanceWidget : GlanceAppWidget(), KoinComponent {

    private val profileRepository: ProfileRepository by lazy { get() }
    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        // Preference Keys
        internal val DATA_STATUS_KEY = stringPreferencesKey("profile_widget_data_status")
        private val BLOOD_TYPE_KEY = stringPreferencesKey("profile_widget_blood_type")
        private val ALLERGIES_KEY = stringPreferencesKey("profile_widget_allergies")
        private val ILLNESSES_KEY = stringPreferencesKey("profile_widget_illnesses")
        private val MEDICATIONS_KEY = stringPreferencesKey("profile_widget_medications")
        private val EMERGENCY_CONTACT_NAME_KEY = stringPreferencesKey("profile_widget_emergency_contact_name")
        private val EMERGENCY_CONTACT_PHONE_KEY = stringPreferencesKey("profile_widget_emergency_contact_phone")
        private val ORGAN_DONOR_KEY = booleanPreferencesKey("profile_widget_organ_donor")
        private val HEIGHT_KEY = stringPreferencesKey("profile_widget_height")
        private val WEIGHT_KEY = stringPreferencesKey("profile_widget_weight")

        // Status Constants
        internal const val STATUS_NEEDS_UPDATE = "NEEDS_UPDATE"
        private const val STATUS_LOADING = "LOADING"
        private const val STATUS_LOADED = "LOADED"
        private const val STATUS_ERROR = "ERROR"
        private const val STATUS_EMPTY = "EMPTY"

        // Coroutine Scope for background tasks initiated by the widget
        private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }


    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Moved prefs and status initialization into provideContent
        // val prefs = currentState()
        // var status = prefs[DATA_STATUS_KEY] ?: STATUS_NEEDS_UPDATE

        // Check if data needs update outside provideContent to trigger the update logic.
        // We'll read the status again inside provideContent for UI decisions.
        val initialPrefs = getAppWidgetState<Preferences>(context, id) // Corrected call
        val initialStatus = initialPrefs[DATA_STATUS_KEY] ?: STATUS_NEEDS_UPDATE

        if (initialStatus == STATUS_NEEDS_UPDATE) {
            // Immediately update status to LOADING to prevent re-triggering fetch logic on recomposition
            // and to show loading UI on this pass.
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { currentPrefs ->
                currentPrefs.toMutablePreferences().apply {
                    this[DATA_STATUS_KEY] = STATUS_LOADING
                }
            }
            // status = STATUS_LOADING // This will be read from prefs inside provideContent

            widgetScope.launch {
                try {
                    val profileState = profileRepository.getProfileData()
                    if (profileState == ProfileScreenState()) { // Default/empty state
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { currentPrefs ->
                            currentPrefs.toMutablePreferences().apply {
                                this[DATA_STATUS_KEY] = STATUS_EMPTY
                                // Clear/default data fields
                                this[BLOOD_TYPE_KEY] = ""
                                this[ALLERGIES_KEY] = ""
                                this[MEDICATIONS_KEY] = ""
                                this[EMERGENCY_CONTACT_NAME_KEY] = ""
                                this[EMERGENCY_CONTACT_PHONE_KEY] = ""
                                this[ORGAN_DONOR_KEY] = false
                                this[HEIGHT_KEY] = ""
                                this[WEIGHT_KEY] = ""
                            }
                        }
                    } else {
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { currentPrefs ->
                            currentPrefs.toMutablePreferences().apply {
                                this[DATA_STATUS_KEY] = STATUS_LOADED
                                this[BLOOD_TYPE_KEY] = profileState.bloodType
                                this[ALLERGIES_KEY] = profileState.allergies
                                this[ILLNESSES_KEY] = profileState.illnesses
                                this[MEDICATIONS_KEY] = profileState.medications
                                this[EMERGENCY_CONTACT_NAME_KEY] = profileState.emergencyContactName
                                this[EMERGENCY_CONTACT_PHONE_KEY] = profileState.emergencyContactPhone
                                this[ORGAN_DONOR_KEY] = profileState.organDonor
                                this[HEIGHT_KEY] = profileState.height
                                this[WEIGHT_KEY] = profileState.weight
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error: Timber.e(e, "Failed to load profile data for widget $id")
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { currentPrefs ->
                        currentPrefs.toMutablePreferences().apply {
                            this[DATA_STATUS_KEY] = STATUS_ERROR
                        }
                    }
                }
            }
        }

        provideContent {
            val prefs = currentState<Preferences>() // Ensure Preferences type is specified
            // Default to STATUS_NEEDS_UPDATE if no status is set yet in Glance state.
            val status = prefs[DATA_STATUS_KEY] ?: STATUS_NEEDS_UPDATE

            DokumedGlanceTheme {
                // If status is NEEDS_UPDATE, the outer logic in provideGlance will set it to LOADING and fetch.
                // So, for rendering, both NEEDS_UPDATE and LOADING can show the loading UI.
                when (status) {
                    STATUS_LOADING, STATUS_NEEDS_UPDATE -> LoadingState()
                    STATUS_LOADED -> {
                        val loadedUiState = ProfileScreenState(
                            bloodType = prefs[BLOOD_TYPE_KEY] ?: "",
                            allergies = prefs[ALLERGIES_KEY] ?: "",
                            illnesses = prefs[ILLNESSES_KEY] ?: "",
                            medications = prefs[MEDICATIONS_KEY] ?: "",
                            emergencyContactName = prefs[EMERGENCY_CONTACT_NAME_KEY] ?: "",
                            emergencyContactPhone = prefs[EMERGENCY_CONTACT_PHONE_KEY] ?: "",
                            organDonor = prefs[ORGAN_DONOR_KEY] ?: false,
                            height = prefs[HEIGHT_KEY] ?: "",
                            weight = prefs[WEIGHT_KEY] ?: ""
                        )
                        ProfileWidgetContent(uiState = loadedUiState)
                    }
                    STATUS_EMPTY -> EmptyState()
                    STATUS_ERROR -> ErrorState()
                    else -> Text("Unknown widget state: $status") // Fallback
                }
            }
        }
    }

    @Composable
    private fun ProfileWidgetItem(label: String, value: String?) {
        if (!value.isNullOrBlank()) {
            Column {
                Text(label, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColorProvider(R.color.glance_text_secondary)))
                Text(value, style = TextStyle(fontSize = 14.sp, color = ColorProvider(R.color.glance_text_primary)))
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun ProfileWidgetContent(uiState: ProfileScreenState) {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.color.widget_background_color) // Background is now handled by DokumedGlanceTheme
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = context.getString(R.string.profile_widget_label),
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp), // Color from theme
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )
                ProfileWidgetItem(label = context.getString(R.string.profile_widget_blood_type_label), value = uiState.bloodType)
                ProfileWidgetItem(label = context.getString(R.string.profile_widget_allergies_label), value = uiState.allergies)
                ProfileWidgetItem(label = context.getString(R.string.profile_widget_ilnesses), value = uiState.illnesses)
                ProfileWidgetItem(label = context.getString(R.string.profile_widget_medications_label), value = uiState.medications)
                ProfileWidgetItem(
                    label = context.getString(R.string.profile_widget_emergency_contact_label),
                    value = if(uiState.emergencyContactName.isNotBlank() && uiState.emergencyContactPhone.isNotBlank()) {
                        "${uiState.emergencyContactName} (${uiState.emergencyContactPhone})"
                    } else if (uiState.emergencyContactPhone.isNotBlank()) {
                        uiState.emergencyContactPhone
                    } else {
                        null
                    }
                )
                ProfileWidgetItem(
                    label = context.getString(R.string.profile_organ_donor),
                    value = if (uiState.organDonor) context.getString(R.string.yes) else context.getString(R.string.no)
                )
                 ProfileWidgetItem(label = context.getString(R.string.profile_height), value = uiState.height)
                 ProfileWidgetItem(label = context.getString(R.string.profile_weight), value = uiState.weight)

            }
        }
    }

    @Composable
    private fun LoadingState() {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp), // Background from theme
            contentAlignment = Alignment.Center
        ) {
            Text(context.getString(R.string.profile_widget_loading)) // Color from theme
        }
    }

    @Composable
    private fun ErrorState() {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp), // Background from theme
            contentAlignment = Alignment.Center
        ) {
            Text(context.getString(R.string.profile_widget_error)) // Color from theme
        }
    }
     @Composable
    private fun EmptyState() {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp), // Background from theme
            contentAlignment = Alignment.Center
        ) {
            Text(context.getString(R.string.profile_widget_data_unavailable)) // Color from theme
        }
    }
}

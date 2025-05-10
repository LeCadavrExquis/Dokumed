package pl.fzar.dokumed.ui.profile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import pl.fzar.dokumed.data.model.MedicalRecord // Keep for fetching records
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import pl.fzar.dokumed.data.repository.ProfileRepository
import pl.fzar.dokumed.receiver.MedicationReminderReceiver
import pl.fzar.dokumed.data.remote.WebDavService // Import the new service
import java.util.Calendar

// Add these constants for the reminder
private const val REMINDER_REQUEST_CODE = 123
const val EXTRA_MEDICATION_NAME = "pl.fzar.dokumed.EXTRA_MEDICATION_NAME"
const val EXTRA_MEDICATION_DOSAGE = "pl.fzar.dokumed.EXTRA_MEDICATION_DOSAGE"

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val webDavService: WebDavService, // Inject WebDavService
    private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenState())
    val uiState: StateFlow<ProfileScreenState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            val profileData = profileRepository.getProfileData()
            _uiState.value = profileData
            // Reschedule alarm if it was enabled
            if (profileData.medicationReminderEnabled && profileData.medicationReminderTime.isNotBlank()) {
                scheduleMedicationReminder(
                    profileData.medicationReminderTime,
                    profileData.medicationName,
                    profileData.medicationDosage
                )
            }
        }
    }

    fun saveProfileData() {
        viewModelScope.launch {
            profileRepository.saveProfileData(_uiState.value)
            if (_uiState.value.medicationReminderEnabled && _uiState.value.medicationReminderTime.isNotBlank()) {
                scheduleMedicationReminder(
                    _uiState.value.medicationReminderTime,
                    _uiState.value.medicationName,
                    _uiState.value.medicationDosage
                )
            } else {
                cancelMedicationReminder()
            }
        }
    }

    private fun scheduleMedicationReminder(time: String, medicationName: String, medicationDosage: String) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, MedicationReminderReceiver::class.java).apply {
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_MEDICATION_DOSAGE, medicationDosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse time, e.g., "HH:mm"
        val timeParts = time.split(":")
        if (timeParts.size != 2) {
            // Handle invalid time format, perhaps log an error or notify user
            // For simplicity, returning here. Consider logging.
            return
        }
        val hour = timeParts[0].toIntOrNull()
        val minute = timeParts[1].toIntOrNull()

        if (hour == null || minute == null) {
            // Handle invalid time format
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time is already past for today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        // Set a repeating alarm for daily reminders.
        // For critical reminders, consider setExactAndAllowWhileIdle and re-scheduling on boot.
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelMedicationReminder() {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, MedicationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Check if it exists
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun onHeightChange(newHeight: String) {
        _uiState.value = _uiState.value.copy(height = newHeight)
    }

    fun onWeightChange(newWeight: String) {
        _uiState.value = _uiState.value.copy(weight = newWeight)
    }

    fun onIllnessesChange(newIllnesses: String) {
        _uiState.value = _uiState.value.copy(illnesses = newIllnesses)
    }

    fun onMedicationsChange(newMedications: String) {
        _uiState.value = _uiState.value.copy(medications = newMedications)
    }

    fun onEmergencyContactNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(emergencyContactName = newName)
    }

    fun onEmergencyContactPhoneChange(newPhone: String) {
        _uiState.value = _uiState.value.copy(emergencyContactPhone = newPhone)
    }

    fun onBloodTypeChange(newBloodType: String) {
        _uiState.value = _uiState.value.copy(bloodType = newBloodType)
    }

    fun onAllergiesChange(newAllergies: String) {
        _uiState.value = _uiState.value.copy(allergies = newAllergies)
    }

    fun onOrganDonorChange(isDonor: Boolean) {
        _uiState.value = _uiState.value.copy(organDonor = isDonor)
    }

    fun onMedicationReminderEnabledChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(medicationReminderEnabled = enabled)
        if (enabled && _uiState.value.medicationReminderTime.isNotBlank()) {
            scheduleMedicationReminder(
                _uiState.value.medicationReminderTime,
                _uiState.value.medicationName,
                _uiState.value.medicationDosage
            )
        } else if (!enabled) {
            cancelMedicationReminder()
        }
    }

    fun onMedicationReminderTimeChange(time: String) {
        _uiState.value = _uiState.value.copy(medicationReminderTime = time)
        if (_uiState.value.medicationReminderEnabled && time.isNotBlank()) {
            scheduleMedicationReminder(
                time,
                _uiState.value.medicationName,
                _uiState.value.medicationDosage
            )
        }
    }

    fun onMedicationNameChange(name: String) {
        _uiState.value = _uiState.value.copy(medicationName = name)
        // Reschedule if reminder is enabled and time is set
        if (_uiState.value.medicationReminderEnabled && _uiState.value.medicationReminderTime.isNotBlank()) {
            scheduleMedicationReminder(
                _uiState.value.medicationReminderTime,
                name,
                _uiState.value.medicationDosage
            )
        }
    }

    fun onMedicationDosageChange(dosage: String) {
        _uiState.value = _uiState.value.copy(medicationDosage = dosage)
        // Reschedule if reminder is enabled and time is set
        if (_uiState.value.medicationReminderEnabled && _uiState.value.medicationReminderTime.isNotBlank()) {
            scheduleMedicationReminder(
                _uiState.value.medicationReminderTime,
                _uiState.value.medicationName,
                dosage
            )
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(medicationName = name)
        // Consider if immediate save is needed or if it's part of a larger "saveProfileData" call
    }

    fun updateBloodType(bloodType: String) {
        _uiState.value = _uiState.value.copy(bloodType = bloodType)
        // Consider if immediate save is needed
    }

    // New handlers for WebDAV fields
    fun onWebdavUrlChange(newUrl: String) {
        _uiState.value = _uiState.value.copy(webdavUrl = newUrl)
    }

    fun onWebdavUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(webdavUsername = newUsername)
    }

    fun onWebdavPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(webdavPassword = newPassword)
    }

    fun syncProfileToWebDAV() {
        viewModelScope.launch {
            val profileData = _uiState.value
            if (profileData.webdavUrl.isBlank() || profileData.webdavUsername.isBlank() || profileData.webdavPassword.isBlank()) {
                _uiState.value = _uiState.value.copy(isSyncing = false, syncStatus = "Error: WebDAV credentials missing.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatus = "Starting sync...")

            // Fetch medical records to pass to the service
            val medicalRecords = medicalRecordRepository.getAllRecordsWithDetails()

            val result = webDavService.syncProfileData(profileData, medicalRecords)

            var statusMessage = result.message
            if (result.profileUploadStatus != null) {
                statusMessage += "\nProfile: ${result.profileUploadStatus}"
            }
            if (result.medicalRecordsCsvUploadStatus != null) {
                statusMessage += "\nRecords CSV: ${result.medicalRecordsCsvUploadStatus}"
            }
            if (result.attachmentsDirStatus != null) {
                statusMessage += "\nAttachments Dir: ${result.attachmentsDirStatus}"
            }
            result.attachmentsUploadStatus?.forEach { attStatus ->
                statusMessage += "\nAttachment: $attStatus"
            }

            _uiState.value = _uiState.value.copy(isSyncing = false, syncStatus = statusMessage)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ktorClient.close() // Close via service if it holds the client
        webDavService.close() // Assuming WebDavService has a close method for its client
    }
}

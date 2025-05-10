package pl.fzar.dokumed.ui.profile

import kotlinx.serialization.Serializable

@Serializable
data class ProfileScreenState(
    val height: String = "",
    val weight: String = "",
    val illnesses: String = "",
    val medications: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val organDonor: Boolean = false,
    val medicationReminderEnabled: Boolean = false,
    val medicationReminderTime: String = "",
    val medicationName: String = "",
    val medicationDosage: String = "",
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "", // Note: This should not be persisted if possible, or handled securely.
    val isSyncing: Boolean = false, // Added for loading state
    val syncStatus: String = "" // Added for status/error messages
)

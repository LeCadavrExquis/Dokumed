package pl.fzar.dokumed.data.remote

import io.ktor.client.statement.HttpResponse
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.ui.profile.ProfileScreenState

interface WebDavService {
    suspend fun syncProfileData(
        profileData: ProfileScreenState,
        medicalRecords: List<MedicalRecord>
    ): SyncResult
    fun close()
}

data class SyncResult(
    val success: Boolean,
    val message: String,
    val profileUploadStatus: String? = null,
    val medicalRecordsCsvUploadStatus: String? = null,
    val attachmentsUploadStatus: List<String>? = null,
    val attachmentsDirStatus: String? = null
)

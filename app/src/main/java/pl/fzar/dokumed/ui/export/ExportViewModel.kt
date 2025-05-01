package pl.fzar.dokumed.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import kotlin.uuid.Uuid

sealed class ExportState {
    object Idle : ExportState()
    object InProgress : ExportState()
    object Success : ExportState()
    data class Error(val message: String) : ExportState()
}

class ExportViewModel(
    private val appContext: Context,
    private val medicalRecordRepository: MedicalRecordRepository
) : ViewModel() {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun exportRecords(recordIds: Set<Uuid>, destinationUri: Uri) {
        // TODO: Implement export logic
    }
}

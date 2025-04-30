package pl.fzar.dokumed.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.clinicalDataRecords
import pl.fzar.dokumed.data.model.consultationRecords
import pl.fzar.dokumed.data.model.measurementRecords
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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
        _exportState.value = ExportState.InProgress
        viewModelScope.launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    recordIds.mapNotNull { id ->
                        medicalRecordRepository.getMedicalRecordById(id)
                    }
                }
                if (records.isEmpty()) {
                    _exportState.value = ExportState.Error("No records selected or found.")
                    return@launch
                }
                val tempDir = File(appContext.cacheDir, "export_temp_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                try {
                    // Write CSV
                    val csvHeader = listOf(
                        "id", "date", "type", "description", "notes", "tags", "doctor", "testName", "value", "unit", "filePath", "fileMimeType"
                    ).joinToString(",")
                    val csvRows = records.map { record ->
                        val base = listOf(
                            record.id.toString(),
                            record.date.toString(),
                            record.type.toString(),
                            record.description?.replace("\n", " ")?.replace(",", ";") ?: "",
                            record.notes?.replace("\n", " ")?.replace(",", ";") ?: "",
                            record.tags.joinToString(";")
                        )
                        val extra = when (record) {
                            is ConsultationRecord -> listOf(
                                record.doctor ?: "",
                                "",
                                "",
                                "",
                                record.filePath ?: "",
                                record.fileMimeType ?: ""
                            )
                            is ClinicalDataRecord -> listOf(
                                "",
                                record.testName,
                                "",
                                "",
                                record.filePath ?: "",
                                record.fileMimeType ?: ""
                            )
                            is pl.fzar.dokumed.data.model.MeasurementRecord -> listOf(
                                "",
                                record.testName,
                                record.value?.toString() ?: "",
                                record.unit ?: "",
                                "",
                                ""
                            )
                            else -> List(6) { "" }
                        }
                        (base + extra).joinToString(",")
                    }
                    val csvData = (listOf(csvHeader) + csvRows).joinToString("\n")
                    val csvFile = File(tempDir, "records.csv")
                    csvFile.writeText(csvData)
                    // Copy attached files
                    records.forEach { record ->
                        val filePath = when (record) {
                            is ConsultationRecord -> record.filePath
                            is ClinicalDataRecord -> record.filePath
                            else -> null
                        }
                        if (!filePath.isNullOrBlank()) {
                            val src = File(filePath)
                            if (src.exists() && src.isFile) {
                                src.copyTo(File(tempDir, src.name), overwrite = true)
                            }
                        }
                    }
                    // Zip
                    appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        ZipOutputStream(BufferedOutputStream(out)).use { zipOut ->
                            tempDir.listFiles()?.forEach { file ->
                                FileInputStream(file).use { fis ->
                                    BufferedInputStream(fis).use { bis ->
                                        val entry = ZipEntry(file.name)
                                        zipOut.putNextEntry(entry)
                                        bis.copyTo(zipOut, 1024)
                                        zipOut.closeEntry()
                                    }
                                }
                            }
                        }
                    } ?: throw Exception("Failed to open output stream for URI.")
                    _exportState.value = ExportState.Success
                } finally {
                    tempDir.deleteRecursively()
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            }
        }
    }
    fun resetState() { _exportState.value = ExportState.Idle }
}

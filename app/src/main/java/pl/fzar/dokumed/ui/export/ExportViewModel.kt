package pl.fzar.dokumed.ui.export

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.Measurement
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.uuid.Uuid
import android.util.Log
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.repository.MedicalRecordRepository

sealed class ExportState {
    object Idle : ExportState()
    object InProgress : ExportState()
    // Modify Success state to include the URI and the email flag
    data class Success(val zipUri: Uri, val sendEmail: Boolean) : ExportState()
    data class Error(val message: String) : ExportState()
}

class ExportViewModel(
    private val appContext: Context,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val tagRepository: pl.fzar.dokumed.data.repository.TagRepository
) : ViewModel() {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    // All records from the repository
    private val _allRecords: MutableStateFlow<List<MedicalRecord>> = MutableStateFlow(emptyList())

    // All tags from the repository
    val allTags: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    init {
        viewModelScope.launch {
            _allRecords.value = medicalRecordRepository.getAllRecordsWithDetails()
            allTags.value = tagRepository.getAllTags().map { it.name }
        }
    }
    // Filter states
    private val _selectedTypes = MutableStateFlow<Set<MedicalRecordType>>(emptySet())
    val selectedTypes: StateFlow<Set<MedicalRecordType>> = _selectedTypes.asStateFlow()

    private val _dateRangeStart = MutableStateFlow<LocalDate?>(null)
    val dateRangeStart: StateFlow<LocalDate?> = _dateRangeStart.asStateFlow()

    private val _dateRangeEnd = MutableStateFlow<LocalDate?>(null)
    val dateRangeEnd: StateFlow<LocalDate?> = _dateRangeEnd.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    // Filtered records based on all records and filter states
    val filteredRecords: StateFlow<List<MedicalRecord>> = combine(
        _allRecords,
        _selectedTypes,
        _dateRangeStart,
        _dateRangeEnd,
        _selectedTags
    ) { records, types, from, to, tags ->
        records.filter { record ->
            (types.isEmpty() || record.type in types) &&
                    (tags.isEmpty() || record.tags.any { it in tags }) &&
                    (from == null || record.date >= from) &&
                    (to == null || record.date <= to)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Add sendEmailAfterExport parameter
    fun exportRecords(recordIds: Set<Uuid>, destinationUri: Uri, sendEmailAfterExport: Boolean) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            Log.i("ExportViewModel", "Starting export for ${recordIds.size} records to $destinationUri. Send email: $sendEmailAfterExport")
            try {
                val records = medicalRecordRepository.getMedicalRecords(recordIds)
                val measurementsMap = medicalRecordRepository.getMeasurementsForRecords(recordIds)
                val clinicalData = medicalRecordRepository.getClinicalDataForRecords(recordIds)

                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
                            addRecordsCsvToZip(zipStream, records)
                            addMeasurementCsvsToZip(zipStream, records, measurementsMap)
                            addFilesToZip(zipStream, clinicalData)
                        }
                    } ?: throw Exception("Failed to open output stream for URI: $destinationUri")
                }
                _exportState.value = ExportState.Success(destinationUri, sendEmailAfterExport)
                Log.i("ExportViewModel", "Export successful to $destinationUri")
            } catch (e: Exception) {
                Log.e("ExportViewModel", "Export failed", e)
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            }
            }
        }

    private fun addRecordsCsvToZip(zipStream: ZipOutputStream, records: List<MedicalRecord>) {
        zipStream.putNextEntry(ZipEntry("records.csv"))
        // Don't use .use here as it closes the underlying zipStream
        val writer = OutputStreamWriter(zipStream)
        // Write header
        writer.appendLine("id,date,type,description") // Adjust columns as needed
        // Write data
        records.forEach { record ->
            // Convert record.type to String before escaping
            writer.appendLine("${record.id},${record.date},\"${escapeCsv(record.type.toString())}\",\"${escapeCsv(record.description ?: "")}\"")
        }
        writer.flush() // Ensure data is written before closing entry
        // DO NOT call writer.close() here
        zipStream.closeEntry()
    }

    // Define a data class for clarity
    private data class MeasurementExportData(
        val description: String,
        val recordId: Uuid,
        val recordDate: LocalDate, // Assuming LocalDate from imports/context
        val measurement: Measurement
    )

    // Updated signature to accept records and the map
    private fun addMeasurementCsvsToZip(
        zipStream: ZipOutputStream,
        records: List<MedicalRecord>,
        measurementsMap: Map<Uuid, List<Measurement>>
    ) {
        // Group measurements using the new data class
        val groupedData: Map<String, List<MeasurementExportData>> = records
            .filter { measurementsMap.containsKey(it.id) } // Only include records with measurements
            .flatMap { record ->
                // Associate each measurement with its parent record's details using the data class
                measurementsMap[record.id]?.map { measurement ->
                    MeasurementExportData(
                        description = record.description ?: "unknown",
                        recordId = record.id,
                        recordDate = record.date,
                        measurement = measurement
                    )
                } ?: emptyList()
            }
            .groupBy { it.description } // Group by description

        groupedData.forEach { (description: String, dataList: List<MeasurementExportData>) -> // Use data class in lambda signature
            val safeDescription = description.replace(Regex("[^a-zA-Z0-9_]"), "_").take(50) // Sanitize filename
            try { // Add try-catch around individual file writing
                zipStream.putNextEntry(ZipEntry("measurement_$safeDescription.csv"))
                // Don't use .use here
                val writer = OutputStreamWriter(zipStream)
                // Write header - Use record_date instead of measurement_timestamp
                writer.appendLine("record_id,record_date,value,unit")
                // Write data
                dataList.forEach { data -> // Iterate over MeasurementExportData
                    // Access data class properties directly
                    writer.appendLine(
                        "${data.recordId},${data.recordDate},${data.measurement.value},\"${escapeCsv(data.measurement.unit ?: "")}\"" // REMOVED extra quote here
                    )
                }
                writer.flush() // Flush before closing entry
                // DO NOT call writer.close() here
                zipStream.closeEntry()
            } catch (e: Exception) {
                 Log.e("ExportViewModel", "Failed to write measurement file for description '$description'", e)
                 // Decide if you want to skip this file or abort the whole export
                 // For now, just log and continue with the next description group
                 // Ensure the entry is closed if opened, though .use should handle the writer
                 try { zipStream.closeEntry() } catch (closeEx: Exception) { /* Ignore */ }
            }
        }
    }


    // Updated parameter type to List<ClinicalData>
    private fun addFilesToZip(zipStream: ZipOutputStream, files: List<ClinicalData>) {
        // Updated loop variable type to ClinicalData
        files.forEach { file: ClinicalData ->
             // No need for inner withContext(Dispatchers.IO) as the whole block runs on IO dispatcher
            try {
                // Assuming ClinicalData has 'uri' (Uri) and 'fileName' (String) properties
                val fileUri = file.filePath?.toUri()
                if (fileUri == null) {
                    Log.w("ExportViewModel", "Skipping file with null path: ${file.fileName}")
                    return@forEach // Skip this file
                }
                appContext.contentResolver.openInputStream(fileUri)?.use { inputStream -> // Use uri
                    zipStream.putNextEntry(ZipEntry("files/${file.fileName}")) // Use fileName
                    BufferedInputStream(inputStream).copyTo(zipStream, 1024)
                    zipStream.closeEntry()
                } ?: Log.w("ExportViewModel", "Could not open input stream for file ${file.fileName} at $fileUri") // Use fileName and Log.w
            } catch (e: Exception) {
                // Log error for specific file, maybe continue export?
                Log.e("ExportViewModel", "Error adding file ${file.fileName} to zip", e) // Use fileName and Log.e
                // Consider adding an error marker or skipping the file
                 // Ensure the entry is closed if opened
                 try { zipStream.closeEntry() } catch (closeEx: Exception) { /* Ignore */ }
            }
        }
    }

     // Simple CSV escaping for quotes
    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    // Optional: Function to reset state manually if needed
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun updateSelectedTypes(types: Set<MedicalRecordType>) {
        _selectedTypes.value = types
    }

    fun updateDateRange(from: LocalDate?, to: LocalDate?) {
        _dateRangeStart.value = from
        _dateRangeEnd.value = to
    }

    fun updateSelectedTags(tags: Set<String>) {
        _selectedTags.value = tags
    }
}

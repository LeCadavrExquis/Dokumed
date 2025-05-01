package pl.fzar.dokumed.ui.export

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.fzar.dokumed.data.model.ClinicalData // Assuming this path
import pl.fzar.dokumed.data.model.Measurement // Assuming this path
import pl.fzar.dokumed.data.model.MedicalRecord // Assuming this path
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.uuid.Uuid
import android.util.Log // Add Log import
import kotlinx.datetime.LocalDate

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
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            Log.i("ExportViewModel", "Starting export for ${recordIds.size} records to $destinationUri") // Added Log
            try {
                // Fetch records, filter out nulls if getMedicalRecordById can return null
                val records = recordIds.map { medicalRecordRepository.getMedicalRecordById(it) }.filterNotNull()
                // Fetch measurements, flatten the map values if necessary
                val measurementsMap = medicalRecordRepository.getMeasurementsForRecords(recordIds)
                val measurements = measurementsMap.values.flatten() // Flatten the list of lists
                // Fetch clinical data (assuming this returns List<ClinicalData>)
                val clinicalData = medicalRecordRepository.getClinicalDataForRecords(recordIds) // Assuming this returns List<ClinicalData>

                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
                            // 1. Add main records CSV
                            addRecordsCsvToZip(zipStream, records) // Pass non-nullable list

                            // 2. Add measurement CSVs (grouped by description)
                            // Pass records and the map to access parent data
                            addMeasurementCsvsToZip(zipStream, records, measurementsMap)

                            // 3. Add attached files (now ClinicalData)
                            addFilesToZip(zipStream, clinicalData) // Pass clinicalData (List<ClinicalData>)
                        }
                    } ?: throw Exception("Failed to open output stream for URI: $destinationUri")
                }
                _exportState.value = ExportState.Success
                Log.i("ExportViewModel", "Export successful to $destinationUri") // Added Log
            } catch (e: Exception) {
                Log.e("ExportViewModel", "Export failed", e) // Use Log.e with exception
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            } finally {
                 // Optionally reset state after a delay or user action
                 // kotlinx.coroutines.delay(3000)
                 // _exportState.value = ExportState.Idle
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
}

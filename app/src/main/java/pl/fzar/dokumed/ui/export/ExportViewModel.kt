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
            } catch (e: Exception) {
                // Log the error properly in a real app
                println("Export failed: ${e.message}") // Simple logging for now
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
        OutputStreamWriter(zipStream).use { writer ->
            // Write header
            writer.appendLine("id,date,type,description") // Adjust columns as needed
            // Write data
            records.forEach { record ->
                // Convert record.type to String before escaping
                writer.appendLine("${record.id},${record.date},\"${escapeCsv(record.type.toString())}\",\"${escapeCsv(record.description ?: "")}\"")
            }
            writer.flush() // Ensure data is written before closing entry
        }
        zipStream.closeEntry()
    }

    // Updated signature to accept records and the map
    private fun addMeasurementCsvsToZip(
        zipStream: ZipOutputStream,
        records: List<MedicalRecord>,
        measurementsMap: Map<Uuid, List<Measurement>>
    ) {
        // Group measurements by parent record's description, keeping track of recordId, recordDate, and measurement
        val groupedData = records
            .filter { measurementsMap.containsKey(it.id) } // Only include records with measurements
            .flatMap { record ->
                // Associate each measurement with its parent record's description, ID, and date
                measurementsMap[record.id]?.map { measurement ->
                    // Use a structure to hold description, recordId, recordDate, measurement
                    // Using Pair<Pair<String, Uuid>, Pair<LocalDate, Measurement>> or a custom class might be cleaner
                    // For simplicity here, using nested Pairs: Pair(Pair(description, recordId), Pair(recordDate, measurement))
                    Pair(Pair(record.description ?: "unknown", record.id), Pair(record.date, measurement))
                } ?: emptyList()
            }
            .groupBy({ it.first.first }) // Group by description (the first element of the outer Pair's first element)

        groupedData.forEach { (description: String, dataList) -> // dataList is List<Pair<Pair<String, Uuid>, Pair<LocalDate, Measurement>>>
            val safeDescription = description.replace(Regex("[^a-zA-Z0-9_]"), "_").take(50) // Sanitize filename
            zipStream.putNextEntry(ZipEntry("measurement_$safeDescription.csv"))
            OutputStreamWriter(zipStream).use { writer ->
                // Write header - Use record_date instead of measurement_timestamp
                writer.appendLine("record_id,record_date,value,unit")
                // Write data
                dataList.forEach { dataPair ->
                    val recordId = dataPair.first.second // Get recordId
                    val recordDate = dataPair.second.first // Get recordDate
                    val measurement = dataPair.second.second // Get measurement
                    // Access measurement properties: value, unit
                    // Access parent recordId and recordDate
                    writer.appendLine(
                        "${recordId},${recordDate},${measurement.value},\"${escapeCsv(measurement.unit ?: "")}\"" // Use recordDate
                    )
                }
                writer.flush()
            }
            zipStream.closeEntry()
        }
    }


    // Updated parameter type to List<ClinicalData>
    private fun addFilesToZip(zipStream: ZipOutputStream, files: List<ClinicalData>) {
        // Updated loop variable type to ClinicalData
        files.forEach { file: ClinicalData ->
             // No need for inner withContext(Dispatchers.IO) as the whole block runs on IO dispatcher
            try {
                // Assuming ClinicalData has 'uri' (Uri) and 'fileName' (String) properties
                appContext.contentResolver.openInputStream(file.filePath!!.toUri())?.use { inputStream -> // Use uri
                    zipStream.putNextEntry(ZipEntry("files/${file.fileName}")) // Use fileName
                    BufferedInputStream(inputStream).copyTo(zipStream, 1024)
                    zipStream.closeEntry()
                } ?: println("Warning: Could not open input stream for file ${file.fileName}") // Use fileName
            } catch (e: Exception) {
                // Log error for specific file, maybe continue export?
                println("Error adding file ${file.fileName} to zip: ${e.message}") // Use fileName
                // Consider adding an error marker or skipping the file
            }
        }
    }

     // Simple CSV escaping for quotes
    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}

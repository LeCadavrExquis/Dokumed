package pl.fzar.dokumed.ui.medicalRecord

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.Measurement
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import pl.fzar.dokumed.data.repository.TagRepository
import pl.fzar.dokumed.util.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.uuid.Uuid

class MedicalRecordViewModel(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val tagRepository: TagRepository,
    private val fileUtil: FileUtil,
) : ViewModel() {

    private val _records = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val records: StateFlow<List<MedicalRecord>> = _records
    
    // For accessing records regardless of filters
    val allRecords: StateFlow<List<MedicalRecord>> = _records

    private val _currentRecord = MutableStateFlow<MedicalRecord?>(null)
    val currentRecord: StateFlow<MedicalRecord?> = _currentRecord

    private val _filteredRecords = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val filteredRecords: StateFlow<List<MedicalRecord>> = _filteredRecords

    private val _selectedTypes = MutableStateFlow<Set<MedicalRecordType>>(emptySet())
    val selectedTypes: StateFlow<Set<MedicalRecordType>> = _selectedTypes

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags

    private val _dateRangeStart = MutableStateFlow<LocalDate?>(null)
    val dateRangeStart: StateFlow<LocalDate?> = _dateRangeStart

    private val _dateRangeEnd = MutableStateFlow<LocalDate?>(null)
    val dateRangeEnd: StateFlow<LocalDate?> = _dateRangeEnd

    val filteredCount: StateFlow<Int> = filteredRecords.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _availableTags
    
    // New state for edit screen
    private val _recordState = MutableStateFlow<RecordOperationState>(RecordOperationState.Idle)
    val recordState: StateFlow<RecordOperationState> = _recordState.asStateFlow()
    
    // State for measurements in edit screen
    private val _measurementsState = MutableStateFlow<List<Measurement>>(emptyList())
    val measurementsState: StateFlow<List<Measurement>> = _measurementsState
    
    // State for clinical data in edit screen
    private val _clinicalDataState = MutableStateFlow<List<ClinicalData>>(emptyList())
    val clinicalDataState: StateFlow<List<ClinicalData>> = _clinicalDataState
    
    // State for clinical data marked for deletion
    private val _clinicalDataForDeletion = MutableStateFlow<Set<Uuid>>(emptySet())

    // State for handling attachments received via Intent
    private val _pendingAttachment = MutableStateFlow<ClinicalData?>(null)
    val pendingAttachment: StateFlow<ClinicalData?> = _pendingAttachment.asStateFlow()

    init {
        // Initialize the flow of records
        viewModelScope.launch {
            medicalRecordRepository.getAllRecords()
                .collect { recordsWithTags ->
                    // Convert recordsWithTags to MedicalRecord objects
                    val mappedRecords = recordsWithTags.map { rwt ->
                        MedicalRecord(
                            id = rwt.medicalRecord.id,
                            date = rwt.medicalRecord.date,
                            type = rwt.medicalRecord.type,
                            description = rwt.medicalRecord.description,
                            notes = rwt.medicalRecord.notes,
                            tags = rwt.tags.map { it.name },
                            // Assuming details are loaded separately or not needed for the list view
                            measurements = emptyList(), // Or load if necessary
                            clinicalData = emptyList(), // Or load if necessary
                            doctor = rwt.medicalRecord.doctor
                        )
                    }
                    _records.value = mappedRecords
                    applyFilters() // Apply filters whenever the base list changes
                }
        }
        // Initialize available tags
        viewModelScope.launch {
            tagRepository.getAllTags().let { tags ->
                _availableTags.value = tags.map { it.name }
            }
        }
    }

    /**
     * Metoda do skopiowania wczytanego pliku do lokalnej pamięci aplikacji.
     * @param uri URI pliku do skopiowania
     * @param fileName Nazwa pliku, pod którą zostanie zapisany w pamięci lokalnej
     * @param onComplete Callback invoked with the absolute path of the saved file upon success
     */
    fun copyFileToLocalStorage(context: Context, uri: Uri, fileName: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Nie udało się otworzyć strumienia wejściowego.")

                val outputDir = context.filesDir // Pamięć wewnętrzna aplikacji
                    ?: throw IOException("Nie udało się znaleźć katalogu do zapisu.")

                // Ensure unique filename if needed, or handle overwrites
                val outputFile = File(outputDir, fileName)

                // Kopiowanie pliku
                copyInputStreamToFile(inputStream, outputFile)

                inputStream.close()

                // Invoke the callback with the absolute path of the saved file
                onComplete(outputFile.absolutePath)

            } catch (e: Exception) {
                e.printStackTrace() // Obsługuje błąd, np. plik może być uszkodzony
                // Optionally, provide error feedback via the callback or another mechanism
            }
        }
    }

    /**
     * Kopiuje dane z InputStream do pliku.
     * @param inputStream Strumień wejściowy
     * @param outputFile Plik wyjściowy
     */
    private fun copyInputStreamToFile(inputStream: InputStream, outputFile: File) {
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
        } finally {
            outputStream?.close()
        }
    }

    private fun loadAllRecords() {
        // This function is now redundant with the initialization flow
        // But keeping it for backward compatibility with other code
        // No implementation needed as the flow will handle updates
    }

    private fun applyFilters() {
        val allRecords = _records.value
        val types = _selectedTypes.value
        val tags = _selectedTags.value
        val from = _dateRangeStart.value
        val to = _dateRangeEnd.value

        _filteredRecords.value = allRecords.filter { record ->
            (types.isEmpty() || record.type in types) &&
                    (tags.isEmpty() || record.tags.any { it in tags }) &&
                    (from == null || record.date >= from) &&
                    (to == null || record.date <= to)
        }
    }

    fun updateSelectedTypes(types: Set<MedicalRecordType>) {
        _selectedTypes.value = types
        applyFilters()
    }

    fun updateSelectedTags(tags: Set<String>) {
        _selectedTags.value = tags
        applyFilters()
    }

    fun updateDateFrom(date: LocalDate?) {
        _dateRangeStart.value = date
        applyFilters()
    }

    fun updateDateTo(date: LocalDate?) {
        _dateRangeEnd.value = date
        applyFilters()
    }

    fun resetFilters() {
        _selectedTypes.value = emptySet()
        _selectedTags.value = emptySet()
        _dateRangeStart.value = null
        _dateRangeEnd.value = null
        applyFilters()
    }

    /**
     * Fetches the specific MedicalRecord using the provided recordId
     * and updates the _currentRecord StateFlow.
     * Assumes a repository method getRecordById exists.
     */
    fun loadRecordDetailsById(recordId: Uuid) {
        viewModelScope.launch {
            _currentRecord.value = null // Clear previous record while loading
            try {
                // Attempt to find the record in the already loaded list first (optimization)
                // Note: This assumes the _records list contains enough detail or you always fetch.
                // If _records only has summary data, you MUST fetch from repo.
                val detailedRecord = medicalRecordRepository.getMedicalRecordById(recordId) // Fetch full details
                _currentRecord.value = detailedRecord

            } catch (e: Exception) {
                // Handle error, e.g., record not found in repository
                _currentRecord.value = null
                // Log the error or show a message if needed
                println("Error loading record details: ${e.message}")
            }
        }
    }

    /**
     * Sets _currentRecord.value to null.
     */
    fun clearCurrentRecord() {
        _currentRecord.value = null
    }

    fun updateRecord(updatedRecord: MedicalRecord) {
        viewModelScope.launch {
            _recordState.value = RecordOperationState.Saving
            try {
                // Ensure all clinical data have unique IDs and correct recordId
                val clinicalDataWithIds = _clinicalDataState.value.map { data ->
                    data.copy(
                        id = data.id ?: kotlin.uuid.Uuid.random(),
                        recordId = updatedRecord.id
                    )
                }
                medicalRecordRepository.updateMedicalRecordWithDetails(
                    updatedRecord,
                    _measurementsState.value,
                    clinicalDataWithIds,
                    _clinicalDataForDeletion.value
                )
                // Update UI state
                _currentRecord.value = updatedRecord
                _clinicalDataForDeletion.value = emptySet()
                _recordState.value = RecordOperationState.Success
                // Log success
                println("Record successfully updated with ID: ${updatedRecord.id}")
            } catch (e: Exception) {
                // Log error and update state
                println("Error updating record: ${e.message}")
                _recordState.value = RecordOperationState.Error("Failed to update record: ${e.message}")
            }
        }
    }

    fun addNewRecord(newRecord: MedicalRecord) {
        viewModelScope.launch {
            _recordState.value = RecordOperationState.Saving
            try {
                // Ensure all clinical data have unique IDs and correct recordId
                val clinicalDataWithIds = _clinicalDataState.value.map { data ->
                    data.copy(
                        id = data.id ?: kotlin.uuid.Uuid.random(),
                        recordId = newRecord.id
                    )
                }
                medicalRecordRepository.insertMedicalRecord(newRecord)
                // Clear states after successful save
                _currentRecord.value = newRecord
                _clinicalDataForDeletion.value = emptySet()
                _recordState.value = RecordOperationState.Success
                // Log success
                println("Record successfully saved with ID: ${newRecord.id}")
            } catch (e: Exception) {
                // Log error and update state
                println("Error saving record: ${e.message}")
                _recordState.value = RecordOperationState.Error("Failed to save record: ${e.message}")
            }
        }
    }

    /**
     * Deletes a medical record from the database.
     * @param recordToDelete The record to be deleted
     */
    fun deleteRecord(recordToDelete: MedicalRecord) {
        viewModelScope.launch {
            _recordState.value = RecordOperationState.Saving // Indicate an operation is in progress
            try {
                medicalRecordRepository.deleteMedicalRecord(recordToDelete)
                // The collect block in init should automatically update _records
                // and trigger applyFilters() if the repository flow emits the new list.
                _recordState.value = RecordOperationState.Success // Indicate success
                println("Record successfully deleted with ID: ${recordToDelete.id}")
            } catch (e: Exception) {
                // Log error and update state
                println("Error deleting record: ${e.message}")
                _recordState.value = RecordOperationState.Error("Failed to delete record: ${e.message}")
            }
        }
    }

    /**
     * Sets a file attachment that should be added to a new record when the
     * edit screen is opened next.
     */
    fun setPendingAttachment(clinicalData: ClinicalData) {
        _pendingAttachment.value = clinicalData
    }

    /**
     * Consumes the pending attachment state, typically after it has been added
     * to the UI state of the edit screen.
     */
    fun consumePendingAttachment() {
        _pendingAttachment.value = null
    }

    // Function to copy file from external URI and prepare ClinicalData
    // Returns the created ClinicalData object or null on failure
    suspend fun copyFileToLocalStorage(context: Context, uri: Uri, mimeType: String?): ClinicalData? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = fileUtil.getFileName(context, uri)
                val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: uri.path?.substringAfterLast('.', "") ?: ""
                val finalFileName = if (fileName.contains('.')) fileName else "$fileName.$fileExtension"

                val destinationPath = fileUtil.copyFileToInternalStorage(context, uri, finalFileName)
                if (destinationPath != null) {
                    ClinicalData(
                        filePath = destinationPath,
                        fileMimeType = mimeType ?: "application/octet-stream",
                        fileName = finalFileName,
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }


    fun loadMedicalRecord(recordId: Uuid?) {
        viewModelScope.launch {
            _recordState.value = RecordOperationState.Loading
            try {
                if (recordId == null) {
                    // For new record, just clear states
                    _currentRecord.value = null
                    _measurementsState.value = emptyList()
                    _clinicalDataState.value = emptyList()
                    _clinicalDataForDeletion.value = emptySet()
                    _recordState.value = RecordOperationState.Idle
                } else {
                    // For existing record, load all data
                    val record = medicalRecordRepository.getMedicalRecordById(recordId)
                    _currentRecord.value = record
                    
                    if (record != null) {
                        // Load measurements
                        val measurements = medicalRecordRepository.getMeasurementsForRecord(recordId)
                        _measurementsState.value = measurements
                        
                        // Load clinical data
                        val clinicalData = medicalRecordRepository.getClinicalDataForRecord(recordId)
                        _clinicalDataState.value = clinicalData
                        
                        _recordState.value = RecordOperationState.Idle
                    } else {
                        _recordState.value = RecordOperationState.Error("Record not found")
                    }
                }
            } catch (e: Exception) {
                _recordState.value = RecordOperationState.Error("Failed to load record: ${e.message}")
            }
        }
    }
    
    fun markClinicalDataForDeletion(clinicalDataId: Uuid) {
        _clinicalDataForDeletion.value = _clinicalDataForDeletion.value + clinicalDataId
        // Also remove from current state to update UI
        _clinicalDataState.value = _clinicalDataState.value.filter { it.id != clinicalDataId }
    }
    
    fun resetSaveState() {
        _recordState.value = RecordOperationState.Idle
    }

    fun updateDateRange(start: LocalDate?, end: LocalDate?) {
        _dateRangeStart.value = start
        _dateRangeEnd.value = end
        applyFilters()
    }
}

// Define operation states for record management
sealed class RecordOperationState {
    object Idle : RecordOperationState()
    object Loading : RecordOperationState()
    object Saving : RecordOperationState()
    object Success : RecordOperationState()
    data class Error(val message: String) : RecordOperationState()
}


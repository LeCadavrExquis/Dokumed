package pl.fzar.dokumed.ui.medicalRecord

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.TagEntity
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.repository.MedicalRecordRepository
import pl.fzar.dokumed.data.repository.TagRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.uuid.Uuid

class MedicalRecordViewModel(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val _records = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val records: StateFlow<List<MedicalRecord>> = _records

    private val _currentRecord = MutableStateFlow<MedicalRecord?>(null)
    val currentRecord: StateFlow<MedicalRecord?> = _currentRecord

    private val _filteredRecords = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val filteredRecords: StateFlow<List<MedicalRecord>> = _filteredRecords

    private val _selectedTypes = MutableStateFlow<Set<MedicalRecordType>>(emptySet())
    val selectedTypes: StateFlow<Set<MedicalRecordType>> = _selectedTypes

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags

    private val _dateFrom = MutableStateFlow<LocalDate?>(null)
    val dateFrom: StateFlow<LocalDate?> = _dateFrom

    private val _dateTo = MutableStateFlow<LocalDate?>(null)
    val dateTo: StateFlow<LocalDate?> = _dateTo

    val filteredCount: StateFlow<Int> = filteredRecords.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags

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
        val from = _dateFrom.value
        val to = _dateTo.value

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
        _dateFrom.value = date
        applyFilters()
    }

    fun updateDateTo(date: LocalDate?) {
        _dateTo.value = date
        applyFilters()
    }

    fun resetFilters() {
        _selectedTypes.value = emptySet()
        _selectedTags.value = emptySet()
        _dateFrom.value = null
        _dateTo.value = null
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
            medicalRecordRepository.updateMedicalRecord(updatedRecord)
            
            // Update tags
            updatedRecord.tags.forEach { tagName ->
                val tag = tagRepository.getTagByName(tagName) ?: TagEntity(id = 0, name = tagName)
                if (tag.id == 0L) {
                    val tagId = tagRepository.insertTag(tag)
                    tagRepository.insertCrossRef(
                        MedicalRecordTagCrossRef(
                            medicalRecordId = updatedRecord.id,
                            tagId = tagId
                        )
                    )
                } else {
                    tagRepository.insertCrossRef(
                        MedicalRecordTagCrossRef(
                            medicalRecordId = updatedRecord.id,
                            tagId = tag.id
                        )
                    )
                }
            }
            
            _currentRecord.value = updatedRecord
        }
    }

    fun addNewRecord(newRecord: MedicalRecord) {
        viewModelScope.launch {
            medicalRecordRepository.insertMedicalRecord(newRecord)

            // DAO logic removed, repository handles all persistence
            
            // Insert tags
            newRecord.tags.forEach { tagName ->
                val tag = tagRepository.getTagByName(tagName) ?: TagEntity(id = 0, name = tagName)
                if (tag.id == 0L) {
                    val tagId = tagRepository.insertTag(tag)
                    tagRepository.insertCrossRef(
                        MedicalRecordTagCrossRef(
                            medicalRecordId = newRecord.id,
                            tagId = tagId
                        )
                    )
                } else {
                    tagRepository.insertCrossRef(
                        MedicalRecordTagCrossRef(
                            medicalRecordId = newRecord.id,
                            tagId = tag.id
                        )
                    )
                }
            }
        }
    }

    /**
     * Deletes a medical record from the database.
     * @param recordToDelete The record to be deleted
     */
    fun deleteRecord(recordToDelete: MedicalRecord) {
        viewModelScope.launch {
            // TODO: Implement logic to delete the record from the database
        }
    }
}
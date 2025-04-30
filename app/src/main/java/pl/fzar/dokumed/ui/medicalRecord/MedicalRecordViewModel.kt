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
import pl.fzar.dokumed.data.dao.MedicalRecordDao
import pl.fzar.dokumed.data.dao.TagDao
import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.TagEntity
import pl.fzar.dokumed.data.entity.toClinicalDataRecord
import pl.fzar.dokumed.data.entity.toConsultationRecord
import pl.fzar.dokumed.data.entity.toMeasurementRecord
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
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
                    _records.value = recordsWithTags.mapNotNull { record ->
                        when (record.medicalRecord.type) {
                            in pl.fzar.dokumed.data.model.consultationRecords -> 
                                record.toConsultationRecord()
                            in pl.fzar.dokumed.data.model.measurementRecords -> 
                                record.toMeasurementRecord()
                            in pl.fzar.dokumed.data.model.clinicalDataRecords -> 
                                record.toClinicalDataRecord()
                            else -> null
                        }
                    }
                    
                    _availableTags.value = _records.value.flatMap { it.tags }.distinct().sorted()
                    applyFilters()
                }
        }
    }

    /**
     * Metoda do skopiowania wczytanego pliku do lokalnej pamięci aplikacji.
     * @param uri URI pliku do skopiowania
     * @param fileName Nazwa pliku, pod którą zostanie zapisany w pamięci lokalnej
     */
    fun copyFileToLocalStorage(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Nie udało się otworzyć strumienia wejściowego.")

                val outputDir = context.filesDir // Pamięć wewnętrzna aplikacji
                    ?: throw IOException("Nie udało się znaleźć katalogu do zapisu.")

                val outputFile = File(outputDir, fileName)

                // Kopiowanie pliku
                copyInputStreamToFile(inputStream, outputFile)

                inputStream.close()

                // Zaktualizowanie ścieżki pliku w rekordzie
                updateFilePathInRecord(fileName, outputFile.absolutePath)

            } catch (e: Exception) {
                e.printStackTrace() // Obsługuje błąd, np. plik może być uszkodzony
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

    /**
     * Metoda do zaktualizowania rekordu z lokalną ścieżką pliku.
     */
    private fun updateFilePathInRecord(fileName: String, filePath: String) {
        // Przykładowa implementacja - należy dostosować do struktury Twoich rekordów
        val updatedRecord = _currentRecord.value?.apply {
            // Zaktualizuj ścieżkę w rekordzie (w zależności od typu rekordu)
            // W przykładzie `filePath` to pole w rekordzie, które przechowuje ścieżkę pliku
        }

        updatedRecord?.let {
            updateRecord(it)
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

    fun loadRecordDetails(type: MedicalRecordType, recordId: Uuid) {
        viewModelScope.launch {
            val record = when (type) {
                in pl.fzar.dokumed.data.model.consultationRecords -> medicalRecordRepository.getConsultationRecordById(recordId)
                in pl.fzar.dokumed.data.model.measurementRecords -> medicalRecordRepository.getMeasurementRecordById(recordId)
                in pl.fzar.dokumed.data.model.clinicalDataRecords -> medicalRecordRepository.getClinicalDataRecordById(recordId)
                else -> null
            }
            _currentRecord.value = record
        }
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
            // Delete any associated files
            if (recordToDelete is ConsultationRecord || recordToDelete is ClinicalDataRecord) {
                val filePath = when (recordToDelete) {
                    is ConsultationRecord -> recordToDelete.filePath
                    is ClinicalDataRecord -> recordToDelete.filePath
                    else -> null
                }
                medicalRecordRepository.deleteAssociatedFile(filePath)
            }
            
            // Delete the record and its tag references
            medicalRecordRepository.deleteMedicalRecord(recordToDelete)
            
            // Clear current record if it was the one deleted
            if (_currentRecord.value?.id == recordToDelete.id) {
                _currentRecord.value = null
            }
        }
    }
}
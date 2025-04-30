package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import kotlin.uuid.Uuid

/**
 * Repository interface for medical records operations
 */
interface MedicalRecordRepository {
    /**
     * Get a flow of all medical records with their tags
     */
    fun getAllRecords(): Flow<List<MedicalRecordWithTags>>
    
    /**
     * Get a medical record by ID
     */
    suspend fun getMedicalRecordById(id: Uuid): MedicalRecord?
    
    /**
     * Get a consultation record by ID
     */
    suspend fun getConsultationRecordById(id: Uuid): ConsultationRecord?
    
    /**
     * Get a clinical data record by ID
     */
    suspend fun getClinicalDataRecordById(id: Uuid): ClinicalDataRecord?
    
    /**
     * Get a measurement record by ID
     */
    suspend fun getMeasurementRecordById(id: Uuid): MeasurementRecord?
    
    /**
     * Insert a new medical record
     */
    suspend fun insertMedicalRecord(record: MedicalRecord)
    
    /**
     * Update an existing medical record
     */
    suspend fun updateMedicalRecord(record: MedicalRecord)
    
    /**
     * Delete a medical record
     */
    suspend fun deleteMedicalRecord(record: MedicalRecord)
    
    /**
     * Delete a file associated with a record if it exists
     */
    suspend fun deleteAssociatedFile(filePath: String?): Boolean
}

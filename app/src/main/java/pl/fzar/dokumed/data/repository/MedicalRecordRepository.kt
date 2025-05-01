package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.flow.Flow
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.model.MedicalRecord
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

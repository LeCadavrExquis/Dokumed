package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.flow.Flow
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.Measurement
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

    /**
     * Get multiple medical records by their IDs
     */
    suspend fun getMedicalRecords(recordIds: Set<Uuid>): List<MedicalRecord>

    /**
     * Get measurements for multiple records
     */
    suspend fun getMeasurementsForRecords(recordIds: Set<Uuid>): Map<Uuid, List<Measurement>>
    
    /**
     * Get clinical data for multiple records
     */
    suspend fun getClinicalDataForRecords(recordIds: Set<Uuid>): List<ClinicalData>
    
    /**
     * Get measurements for a single record
     */
    suspend fun getMeasurementsForRecord(recordId: Uuid): List<Measurement>
    
    /**
     * Get clinical data for a single record
     */
    suspend fun getClinicalDataForRecord(recordId: Uuid): List<ClinicalData>
    
    /**
     * Insert a medical record with all its related details
     */
    suspend fun insertMedicalRecordWithDetails(
        record: MedicalRecord, 
        measurements: List<Measurement>, 
        clinicalData: List<ClinicalData>
    )
    
    /**
     * Update a medical record with all its related details
     */
    suspend fun updateMedicalRecordWithDetails(
        record: MedicalRecord, 
        measurements: List<Measurement>, 
        clinicalData: List<ClinicalData>,
        clinicalDataToDelete: Set<Uuid>
    )
}

object DummyMedicalRecordRepository : MedicalRecordRepository {
    override fun getAllRecords(): Flow<List<MedicalRecordWithTags>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun getMedicalRecordById(id: Uuid): MedicalRecord? = null
    override suspend fun insertMedicalRecord(record: MedicalRecord) {}
    override suspend fun updateMedicalRecord(record: MedicalRecord) {}
    override suspend fun deleteMedicalRecord(record: MedicalRecord) {}
    override suspend fun deleteAssociatedFile(filePath: String?): Boolean = false
    override suspend fun getMedicalRecords(recordIds: Set<Uuid>): List<MedicalRecord> = emptyList()
    override suspend fun getMeasurementsForRecords(recordIds: Set<Uuid>): Map<Uuid, List<Measurement>> = emptyMap()
    override suspend fun getClinicalDataForRecords(recordIds: Set<Uuid>): List<ClinicalData> = emptyList()
    override suspend fun getMeasurementsForRecord(recordId: Uuid): List<Measurement> = emptyList()
    override suspend fun getClinicalDataForRecord(recordId: Uuid): List<ClinicalData> = emptyList()
    override suspend fun insertMedicalRecordWithDetails(record: MedicalRecord, measurements: List<Measurement>, clinicalData: List<ClinicalData>) {}
    override suspend fun updateMedicalRecordWithDetails(record: MedicalRecord, measurements: List<Measurement>, clinicalData: List<ClinicalData>, clinicalDataToDelete: Set<Uuid>) {}
}

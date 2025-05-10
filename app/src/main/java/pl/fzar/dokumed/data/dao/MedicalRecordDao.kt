package pl.fzar.dokumed.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pl.fzar.dokumed.data.entity.ClinicalDataEntity
import pl.fzar.dokumed.data.entity.MeasurementEntity
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordWithDetails
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import kotlin.uuid.Uuid

@Dao
interface MedicalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicalRecord: MedicalRecordEntity)

    @Update
    suspend fun update(medicalRecord: MedicalRecordEntity)

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getMedicalRecordById(id: Uuid): MedicalRecordEntity?

    @Transaction
    @Query("SELECT * FROM medical_records")
    suspend fun getAllMedicalRecords(): List<MedicalRecordEntity>

    @Transaction
    @Query("SELECT * FROM medical_records")
    fun getAllRecords(): Flow<List<MedicalRecordWithTags>>

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteMedicalRecord(id: Uuid)

    @Transaction
    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getMedicalRecordWithDetails(id: Uuid): MedicalRecordWithDetails?

    @Transaction
    @Query("SELECT * FROM medical_records WHERE id IN (:recordIds)")
    suspend fun getMedicalRecordsWithDetails(recordIds: Set<Uuid>): List<MedicalRecordWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalData(clinicalData: ClinicalDataEntity)

    @Query("DELETE FROM measurements WHERE medicalRecordId = :medicalRecordId")
    suspend fun deleteMeasurementsForRecord(medicalRecordId: Uuid)

    @Query("DELETE FROM clinical_data WHERE medicalRecordId = :medicalRecordId")
    suspend fun deleteClinicalDataForRecord(medicalRecordId: Uuid)

    @Query("SELECT * FROM measurements WHERE medicalRecordId IN (:recordIds)")
    suspend fun getMeasurementsForRecords(recordIds: Set<Uuid>): List<MeasurementEntity>

    @Query("SELECT * FROM clinical_data WHERE medicalRecordId IN (:recordIds)")
    suspend fun getClinicalDataForRecords(recordIds: Set<Uuid>): List<ClinicalDataEntity>

    @Transaction
    @Query("SELECT * FROM clinical_data WHERE id IN (:ids)")
    suspend fun getClinicalDataByIds(ids: Set<Uuid>): List<ClinicalDataEntity>

    @Query("DELETE FROM clinical_data WHERE id = :id")
    suspend fun deleteClinicalDataById(id: Uuid)
}
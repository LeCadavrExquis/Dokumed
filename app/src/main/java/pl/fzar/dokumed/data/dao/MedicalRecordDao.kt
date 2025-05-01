package pl.fzar.dokumed.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: pl.fzar.dokumed.data.entity.MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalData(clinicalData: pl.fzar.dokumed.data.entity.ClinicalDataEntity)

    @Query("DELETE FROM measurements WHERE medicalRecordId = :medicalRecordId")
    suspend fun deleteMeasurementsForRecord(medicalRecordId: Uuid)

    @Query("DELETE FROM clinical_data WHERE medicalRecordId = :medicalRecordId")
    suspend fun deleteClinicalDataForRecord(medicalRecordId: Uuid)
}
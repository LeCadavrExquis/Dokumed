package pl.fzar.dokumed.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.fzar.dokumed.data.entity.ClinicalDataRecordEntity
import pl.fzar.dokumed.data.entity.ClinicalDataRecordWithDetails
import pl.fzar.dokumed.data.entity.ConsultationRecordEntity
import pl.fzar.dokumed.data.entity.ConsultationRecordWithDetails
import pl.fzar.dokumed.data.entity.MeasurementRecordEntity
import pl.fzar.dokumed.data.entity.MeasurementRecordWithDetails
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.model.MedicalRecord
import kotlin.uuid.Uuid

@Dao
interface MedicalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicalRecord: MedicalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consultationRecord: ConsultationRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clinicalDataRecord: ClinicalDataRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurementRecord: MeasurementRecordEntity)

    @Update
    suspend fun update(medicalRecord: MedicalRecordEntity)

    @Update
    suspend fun update(consultationRecord: ConsultationRecordEntity)

    @Update
    suspend fun update(clinicalDataRecord: ClinicalDataRecordEntity)

    @Update
    suspend fun update(measurementRecord: MeasurementRecordEntity)

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getMedicalRecordById(id: Uuid): MedicalRecordEntity?

    @Transaction
    @Query("SELECT * FROM consultation_records WHERE id = :id")
    suspend fun getConsultationRecordById(id: Uuid): ConsultationRecordEntity?

    @Transaction
    @Query("SELECT * FROM clinical_data_records WHERE id = :id")
    suspend fun getClinicalDataRecordById(id: Uuid): ClinicalDataRecordEntity?

    @Transaction
    @Query("SELECT * FROM measurement_records WHERE id = :id")
    suspend fun getMeasurementRecordById(id: Uuid): MeasurementRecordEntity?

    @Transaction
    @Query("SELECT * FROM consultation_records WHERE medicalRecordId = :medicalRecordId")
    suspend fun getConsultationRecordWithDetails(medicalRecordId: Uuid): ConsultationRecordWithDetails?

    @Transaction
    @Query("SELECT * FROM clinical_data_records WHERE medicalRecordId = :medicalRecordId")
    suspend fun getClinicalDataRecordWithDetails(medicalRecordId: Uuid): ClinicalDataRecordWithDetails?

    @Transaction
    @Query("SELECT * FROM measurement_records WHERE medicalRecordId = :medicalRecordId")
    suspend fun getMeasurementRecordWithDetails(medicalRecordId: Uuid): MeasurementRecordWithDetails?

    @Transaction
    @Query("SELECT * FROM medical_records")
    suspend fun getAllMedicalRecords(): List<MedicalRecordEntity>

    @Transaction
    @Query("""
    SELECT mr.*, GROUP_CONCAT(t.name) AS tags
    FROM medical_records mr
    LEFT JOIN medical_record_tag_cross_ref mrc ON mr.id = mrc.medicalRecordId
    LEFT JOIN tags t ON mrc.tagId = t.id
    GROUP BY mr.id
""")
    suspend fun getAllMedicalRecordsWithTags(): List<MedicalRecordWithTags>

    @Transaction
    @Query("""
    SELECT mr.*, GROUP_CONCAT(t.name) AS tags
    FROM medical_records mr
    LEFT JOIN medical_record_tag_cross_ref mrc ON mr.id = mrc.medicalRecordId
    LEFT JOIN tags t ON mrc.tagId = t.id
    WHERE mr.id = :recordId
    GROUP BY mr.id
""")
    suspend fun getMedicalRecordWithTagsById(recordId: Uuid): MedicalRecordWithTags?

    @Transaction
    @Query("SELECT * FROM medical_records")
    fun getAllRecords(): kotlinx.coroutines.flow.Flow<List<MedicalRecordWithTags>>

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteMedicalRecord(id: Uuid)

    @Query("DELETE FROM consultation_records WHERE id = :id")
    suspend fun deleteConsultationRecordById(id: Uuid)

    @Query("DELETE FROM clinical_data_records WHERE id = :id")
    suspend fun deleteClinicalDataRecordById(id: Uuid)

    @Query("DELETE FROM measurement_records WHERE id = :id")
    suspend fun deleteMeasurementRecordById(id: Uuid)

    suspend fun deleteConsultationRecord(record: ConsultationRecordEntity) {
        deleteConsultationRecordById(record.id)
    }

    suspend fun deleteClinicalDataRecord(record: ClinicalDataRecordEntity) {
        deleteClinicalDataRecordById(record.id)
    }

    suspend fun deleteMeasurementRecord(record: MeasurementRecordEntity) {
        deleteMeasurementRecordById(record.id)
    }
}
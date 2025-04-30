package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.dao.MedicalRecordDao
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.entity.toClinicalDataRecord
import pl.fzar.dokumed.data.entity.toConsultationRecord
import pl.fzar.dokumed.data.entity.toMeasurementRecord
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
import pl.fzar.dokumed.data.model.MedicalRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.toClinicalDataRecordEntity
import pl.fzar.dokumed.data.model.toConsultationRecordEntity
import pl.fzar.dokumed.data.model.toMeasurementRecordEntity
import pl.fzar.dokumed.data.model.toMedicalRecordEntity
import java.io.File
import kotlin.uuid.Uuid

/**
 * Implementation of the MedicalRecordRepository interface
 */
class MedicalRecordRepositoryImpl(
    private val medicalRecordDao: MedicalRecordDao
) : MedicalRecordRepository {

    override fun getAllRecords(): Flow<List<MedicalRecordWithTags>> {
        return medicalRecordDao.getAllRecords()
    }

    override suspend fun getMedicalRecordById(id: Uuid): MedicalRecord? {
        val record = medicalRecordDao.getMedicalRecordWithTagsById(id) ?: return null
        return when (record.medicalRecord.type) {
            in pl.fzar.dokumed.data.model.consultationRecords -> {
                val consultationRecord = medicalRecordDao.getConsultationRecordWithDetails(id)
                consultationRecord?.toConsultationRecord()
            }
            in pl.fzar.dokumed.data.model.clinicalDataRecords -> {
                val clinicalDataRecord = medicalRecordDao.getClinicalDataRecordWithDetails(id)
                clinicalDataRecord?.toClinicalDataRecord()
            }
            in pl.fzar.dokumed.data.model.measurementRecords -> {
                val measurementRecord = medicalRecordDao.getMeasurementRecordWithDetails(id)
                measurementRecord?.toMeasurementRecord()
            }
            else -> null
        }
    }

    override suspend fun getConsultationRecordById(id: Uuid): ConsultationRecord? {
        val consultationRecord = medicalRecordDao.getConsultationRecordWithDetails(id) ?: return null
        return consultationRecord.toConsultationRecord()
    }

    override suspend fun getClinicalDataRecordById(id: Uuid): ClinicalDataRecord? {
        val clinicalDataRecord = medicalRecordDao.getClinicalDataRecordWithDetails(id) ?: return null
        return clinicalDataRecord.toClinicalDataRecord()
    }

    override suspend fun getMeasurementRecordById(id: Uuid): MeasurementRecord? {
        val measurementRecord = medicalRecordDao.getMeasurementRecordWithDetails(id) ?: return null
        return measurementRecord.toMeasurementRecord()
    }

    override suspend fun insertMedicalRecord(record: MedicalRecord) {
        val medicalRecordEntity = when (record) {
            is ConsultationRecord -> record.toMedicalRecordEntity(record.id)
            is ClinicalDataRecord -> record.toMedicalRecordEntity(record.id)
            is MeasurementRecord -> record.toMedicalRecordEntity(record.id)
            else -> throw IllegalArgumentException("Unknown record type")
        }
        medicalRecordDao.insert(medicalRecordEntity)

        when (record) {
            is ConsultationRecord -> {
                val consultationEntity = record.toConsultationRecordEntity(record.id)
                medicalRecordDao.insert(consultationEntity)
            }
            is ClinicalDataRecord -> {
                val clinicalDataEntity = record.toClinicalDataRecordEntity(record.id)
                medicalRecordDao.insert(clinicalDataEntity)
            }
            is MeasurementRecord -> {
                val measurementEntity = record.toMeasurementRecordEntity(record.id)
                medicalRecordDao.insert(measurementEntity)
            }
        }
    }

    override suspend fun updateMedicalRecord(record: MedicalRecord) {
        val medicalRecordEntity = when (record) {
            is ConsultationRecord -> record.toMedicalRecordEntity(record.id)
            is ClinicalDataRecord -> record.toMedicalRecordEntity(record.id)
            is MeasurementRecord -> record.toMedicalRecordEntity(record.id)
            else -> throw IllegalArgumentException("Unknown record type")
        }
        medicalRecordDao.update(medicalRecordEntity)

        when (record) {
            is ConsultationRecord -> {
                val consultationEntity = record.toConsultationRecordEntity(record.id)
                medicalRecordDao.update(consultationEntity)
            }
            is ClinicalDataRecord -> {
                val clinicalDataEntity = record.toClinicalDataRecordEntity(record.id)
                medicalRecordDao.update(clinicalDataEntity)
            }
            is MeasurementRecord -> {
                val measurementEntity = record.toMeasurementRecordEntity(record.id)
                medicalRecordDao.update(measurementEntity)
            }
        }
    }

    override suspend fun deleteMedicalRecord(record: MedicalRecord) {
        when (record) {
            is ConsultationRecord -> {
                val existingConsultationEntity = medicalRecordDao.getConsultationRecordById(record.id)
                if (existingConsultationEntity != null) {
                    medicalRecordDao.deleteConsultationRecord(existingConsultationEntity)
                    medicalRecordDao.deleteMedicalRecord(record.id)
                }
            }
            is ClinicalDataRecord -> {
                val existingClinicalDataEntity = medicalRecordDao.getClinicalDataRecordById(record.id)
                if (existingClinicalDataEntity != null) {
                    medicalRecordDao.deleteClinicalDataRecord(existingClinicalDataEntity)
                    medicalRecordDao.deleteMedicalRecord(record.id)
                }
            }
            is MeasurementRecord -> {
                val existingMeasurementEntity = medicalRecordDao.getMeasurementRecordById(record.id)
                if (existingMeasurementEntity != null) {
                    medicalRecordDao.deleteMeasurementRecord(existingMeasurementEntity)
                    medicalRecordDao.deleteMedicalRecord(record.id)
                }
            }
        }
    }

    override suspend fun deleteAssociatedFile(filePath: String?): Boolean {
        return try {
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.flow.Flow
import pl.fzar.dokumed.data.dao.MedicalRecordDao
import pl.fzar.dokumed.data.entity.ClinicalDataEntity
import pl.fzar.dokumed.data.entity.MeasurementEntity
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.entity.TagEntity
import pl.fzar.dokumed.data.model.ClinicalData
import pl.fzar.dokumed.data.model.Measurement
import pl.fzar.dokumed.data.model.MedicalRecord
import java.io.File
import kotlin.uuid.Uuid

class MedicalRecordRepositoryImpl(
    private val medicalRecordDao: MedicalRecordDao,
    private val tagRepository: TagRepository
) : MedicalRecordRepository {

    override fun getAllRecords(): Flow<List<MedicalRecordWithTags>> {
        return medicalRecordDao.getAllRecords()
    }

    override suspend fun getMedicalRecordById(id: Uuid): MedicalRecord? {
        val details = medicalRecordDao.getMedicalRecordWithDetails(id) ?: return null
        val entity = details.medicalRecord
        val tags = details.tags.map { it.name }
        val measurements = details.measurements.map {
            Measurement(
                value = it.value,
                unit = it.unit
            )
        }
        val clinicalData = details.clinicalData.map {
            ClinicalData(
                filePath = it.filePath,
                fileMimeType = it.fileMimeType,
            )
        }
        return MedicalRecord(
            id = entity.id,
            date = entity.date,
            type = entity.type,
            description = entity.description,
            notes = entity.notes,
            tags = tags,
            measurements = measurements,
            clinicalData = clinicalData,
            doctor = entity.doctor
        )
    }

    override suspend fun insertMedicalRecord(record: MedicalRecord) {
        val entity = pl.fzar.dokumed.data.entity.MedicalRecordEntity(
            id = record.id,
            date = record.date,
            type = record.type,
            doctor = null, // doctor is not in MedicalRecord, only in ConsultationRecord
            description = record.description,
            notes = record.notes
        )
        medicalRecordDao.insert(entity)

        // Insert measurements
        record.measurements.forEach { m ->
            val measurementEntity = MeasurementEntity(
                id = Uuid.random(),
                medicalRecordId = record.id,
                value = m.value,
                unit = m.unit
            )
            medicalRecordDao.insertMeasurement(measurementEntity)
        }

        // Insert clinical data
        record.clinicalData.forEach { c ->
            val clinicalDataEntity = ClinicalDataEntity(
                id = Uuid.random(),
                medicalRecordId = record.id,
                filePath = c.filePath,
                fileMimeType = c.fileMimeType,
            )
            medicalRecordDao.insertClinicalData(clinicalDataEntity)
        }

        // Insert tags and cross-refs
        for (tagName in record.tags) {
            val tag = tagRepository.getTagByName(tagName) ?: TagEntity(name = tagName)
            val tagId = if (tag.id == 0L) tagRepository.insertTag(tag) else tag.id
            tagRepository.insertCrossRef(MedicalRecordTagCrossRef(record.id, tagId))
        }
    }

    override suspend fun updateMedicalRecord(record: MedicalRecord) {
        val entity = MedicalRecordEntity(
            id = record.id,
            date = record.date,
            type = record.type,
            doctor = null,
            description = record.description,
            notes = record.notes
        )
        medicalRecordDao.update(entity)

        // Remove old measurements/clinicalData and re-insert
        medicalRecordDao.deleteMeasurementsForRecord(record.id)
        medicalRecordDao.deleteClinicalDataForRecord(record.id)

        record.measurements.forEach { m ->
            val measurementEntity = MeasurementEntity(
                id = Uuid.random(),
                medicalRecordId = record.id,
                value = m.value,
                unit = m.unit
            )
            medicalRecordDao.insertMeasurement(measurementEntity)
        }
        record.clinicalData.forEach { c ->
            val clinicalDataEntity = ClinicalDataEntity(
                id = Uuid.random(),
                medicalRecordId = record.id,
                filePath = c.filePath,
                fileMimeType = c.fileMimeType,
            )
            medicalRecordDao.insertClinicalData(clinicalDataEntity)
        }

        // Update tags
        tagRepository.deleteCrossRefsForMedicalRecord(record.id)
        for (tagName in record.tags) {
            val tag = tagRepository.getTagByName(tagName) ?: TagEntity(name = tagName)
            val tagId = if (tag.id == 0L) tagRepository.insertTag(tag) else tag.id
            tagRepository.insertCrossRef(MedicalRecordTagCrossRef(record.id, tagId))
        }
    }

    override suspend fun deleteMedicalRecord(record: MedicalRecord) {
        // Remove tag cross-refs
        tagRepository.deleteCrossRefsForMedicalRecord(record.id)
        // Remove measurements and clinical data
        medicalRecordDao.deleteMeasurementsForRecord(record.id)
        medicalRecordDao.deleteClinicalDataForRecord(record.id)
        // Remove the record itself
        medicalRecordDao.deleteMedicalRecord(record.id)
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

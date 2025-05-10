package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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

    override suspend fun getAllRecordsWithDetails(): List<MedicalRecord> {
        // Get all record entities
        val allEntities = medicalRecordDao.getAllMedicalRecords()
        if (allEntities.isEmpty()) return emptyList()
        val recordIds = allEntities.map { it.id }.toSet()
        // Get all details for all records
        val detailsList = medicalRecordDao.getMedicalRecordsWithDetails(recordIds)
        return detailsList.map { details ->
            val entity = details.medicalRecord
            val tags = details.tags.map { it.name }
            val measurements = details.measurements.map {
                pl.fzar.dokumed.data.model.Measurement(
                    value = it.value,
                    unit = it.unit
                )
            }
            val clinicalData = details.clinicalData.map {
                pl.fzar.dokumed.data.model.ClinicalData(
                    id = it.id,
                    recordId = it.medicalRecordId,
                    filePath = it.filePath,
                    fileMimeType = it.fileMimeType
                )
            }
            pl.fzar.dokumed.data.model.MedicalRecord(
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
                id = it.id,
                recordId = it.medicalRecordId,
                filePath = it.filePath,
                fileMimeType = it.fileMimeType
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
                id = c.id ?: Uuid.random(),
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
        // Use withTransaction for atomicity

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
        // Insert clinical data
        record.clinicalData.forEach { c ->
            val clinicalDataEntity = ClinicalDataEntity(
                id = c.id ?: Uuid.random(), // Use existing ID or generate new if null
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
        // Use withContext(Dispatchers.IO) for file operations
        withContext(Dispatchers.IO) {
            // Delete associated files first
            record.clinicalData.forEach { clinicalDataItem ->
                clinicalDataItem.filePath?.let {
                    deleteAssociatedFile(it) // Call the existing method to delete the file
                }
            }
        }
        // Then, proceed to delete database entries
        tagRepository.deleteCrossRefsForMedicalRecord(record.id)
        medicalRecordDao.deleteMeasurementsForRecord(record.id)
        medicalRecordDao.deleteClinicalDataForRecord(record.id) // Deletes all clinical data for the record
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

    override suspend fun getMedicalRecords(recordIds: Set<Uuid>): List<MedicalRecord> {
        val detailsList = medicalRecordDao.getMedicalRecordsWithDetails(recordIds)
        return detailsList.map { details ->
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
                    id = it.id,
                    recordId = it.medicalRecordId,
                    filePath = it.filePath,
                    fileMimeType = it.fileMimeType
                )
            }
            MedicalRecord(
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
    }

    override suspend fun getMeasurementsForRecords(recordIds: Set<Uuid>): Map<Uuid, List<Measurement>> {
        val measurementEntities = medicalRecordDao.getMeasurementsForRecords(recordIds)
        return measurementEntities.groupBy { it.medicalRecordId }
            .mapValues { entry ->
                entry.value.map { entity ->
                    Measurement(value = entity.value, unit = entity.unit)
                }
            }
    }

    override suspend fun getClinicalDataForRecords(recordIds: Set<Uuid>): List<ClinicalData> {
        val clinicalDataEntities = medicalRecordDao.getClinicalDataForRecords(recordIds)
        return clinicalDataEntities.map { entity ->
            ClinicalData(
                id = entity.id,
                recordId = entity.medicalRecordId,
                filePath = entity.filePath,
                fileMimeType = entity.fileMimeType
            )
        }
    }

    override suspend fun getMeasurementsForRecord(recordId: Uuid): List<Measurement> {
        val measurementEntities = medicalRecordDao.getMeasurementsForRecords(setOf(recordId))
        return measurementEntities.filter { it.medicalRecordId == recordId }
            .map { entity ->
                Measurement(value = entity.value, unit = entity.unit)
            }
    }

    override suspend fun getClinicalDataForRecord(recordId: Uuid): List<ClinicalData> {
        val clinicalDataEntities = medicalRecordDao.getClinicalDataForRecords(setOf(recordId))
        return clinicalDataEntities.filter { it.medicalRecordId == recordId }
            .map { entity ->
                ClinicalData(
                    id = entity.id,
                    recordId = entity.medicalRecordId,
                    filePath = entity.filePath,
                    fileMimeType = entity.fileMimeType
                )
            }
    }

    override suspend fun insertMedicalRecordWithDetails(
        record: MedicalRecord,
        measurements: List<Measurement>,
        clinicalData: List<ClinicalData>
    ) {
        // Insert the record entity
        val entity = MedicalRecordEntity(
            id = record.id,
            date = record.date,
            type = record.type,
            doctor = record.doctor,
            description = record.description,
            notes = record.notes
        )
        medicalRecordDao.insert(entity)

        // Insert measurements
        measurements.forEach { m ->
            val measurementEntity = MeasurementEntity(
                id = Uuid.random(),
                medicalRecordId = record.id,
                value = m.value,
                unit = m.unit
            )
            medicalRecordDao.insertMeasurement(measurementEntity)
        }

        // Insert clinical data
        clinicalData.forEach { c ->
            val clinicalDataEntity = ClinicalDataEntity(
                id = c.id ?: Uuid.random(),
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

    override suspend fun updateMedicalRecordWithDetails(
        record: MedicalRecord,
        measurements: List<Measurement>,
        clinicalData: List<ClinicalData>,
        clinicalDataToDelete: Set<Uuid> // IDs of ClinicalDataEntity to delete
    ) {
        // Fetch the ClinicalDataEntities to be deleted to get their filePaths
        if (clinicalDataToDelete.isNotEmpty()) {
            val clinicalDataEntitiesToDelete = medicalRecordDao.getClinicalDataByIds(clinicalDataToDelete)
            withContext(Dispatchers.IO) {
                clinicalDataEntitiesToDelete.forEach { entity ->
                    entity.filePath?.let { deleteAssociatedFile(it) }
                }
            }
            // Delete the database entries for these specific clinical data items
            clinicalDataToDelete.forEach { id ->
                medicalRecordDao.deleteClinicalDataById(id)
            }
        }

        // Update the record entity
        val entity = MedicalRecordEntity(
            id = record.id,
            date = record.date,
            type = record.type,
            doctor = record.doctor,
            description = record.description,
            notes = record.notes
        )
        medicalRecordDao.update(entity)

        // Clear and re-insert measurements (or implement more granular update)
        medicalRecordDao.deleteMeasurementsForRecord(record.id)
        record.measurements.forEach { m ->
            val measurementEntity = MeasurementEntity(
                id = Uuid.random(),
                medicalRecordId = record.id,
                value = m.value,
                unit = m.unit
            )
            medicalRecordDao.insertMeasurement(measurementEntity)
        }

        // Upsert clinical data (those not in clinicalDataToDelete)
        // We assume clinicalData list contains items to be kept or added.
        // If an item in clinicalData has an ID, it might be an update, otherwise new.
        // For simplicity, current logic in MedicalRecordViewModel seems to replace all, which is fine.
        // Here, we ensure only the remaining/new ones are inserted.
        // First, delete all existing clinical data for the record that were not explicitly marked for deletion earlier.
        // This step might be redundant if the calling ViewModel manages the full list.
        // However, to be safe and ensure only `clinicalData` list items persist:
        // medicalRecordDao.deleteClinicalDataForRecord(record.id) // This would delete ALL, be careful.
        // Instead, rely on the `clinicalDataToDelete` for specific deletions.
        // Then, insert the items present in the `clinicalData` list.

        record.clinicalData.forEach { c ->
            val clinicalDataEntity = ClinicalDataEntity(
                id = c.id ?: Uuid.random(),
                medicalRecordId = record.id,
                filePath = c.filePath,
                fileMimeType = c.fileMimeType
            )
            medicalRecordDao.insertClinicalData(clinicalDataEntity) // This will replace if ID exists due to OnConflictStrategy.REPLACE
        }

        // Update tags
        tagRepository.deleteCrossRefsForMedicalRecord(record.id)
        for (tagName in record.tags) {
            val tag = tagRepository.getTagByName(tagName) ?: TagEntity(name = tagName)
            val tagId = if (tag.id == 0L) tagRepository.insertTag(tag) else tag.id
            tagRepository.insertCrossRef(MedicalRecordTagCrossRef(record.id, tagId))
        }
    }

    // Implementation for the new method
    override suspend fun deleteClinicalDataItem(clinicalDataId: Uuid, filePath: String?) {
        withContext(Dispatchers.IO) {
            filePath?.let {
                deleteAssociatedFile(it)
            }
        }
        medicalRecordDao.deleteClinicalDataById(clinicalDataId)
    }
}

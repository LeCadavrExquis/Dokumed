package pl.fzar.dokumed.data.model

import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.entity.ClinicalDataRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import kotlin.uuid.Uuid

data class ClinicalDataRecord(
    override val id: Uuid = Uuid.random(),
    override val date: LocalDate,
    override val type: MedicalRecordType,
    override val description: String?,
    override val notes: String?,
    override val tags: List<String> = emptyList(),
    val filePath: String? = null,
    val fileMimeType: String? = null,
    val testName: String,
) : MedicalRecord(id, date, type, description, notes, tags)

fun ClinicalDataRecord.toMedicalRecordEntity(medicalRecordId: Uuid): MedicalRecordEntity {
    return MedicalRecordEntity(
        id = medicalRecordId,
        date = this.date,
        type = this.type,
        description = this.description,
        notes = this.notes,
    )
}

fun ClinicalDataRecord.toClinicalDataRecordEntity(medicalRecordId: Uuid): ClinicalDataRecordEntity {
    return ClinicalDataRecordEntity(
        id = this.id,
        medicalRecordId = medicalRecordId,
        filePath = this.filePath,
        fileMimeType = this.fileMimeType,
        testName = this.testName,
    )
}
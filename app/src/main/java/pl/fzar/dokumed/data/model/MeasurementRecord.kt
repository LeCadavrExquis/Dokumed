package pl.fzar.dokumed.data.model

import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.entity.MeasurementRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import kotlin.uuid.Uuid

data class MeasurementRecord(
    override val id: Uuid = Uuid.random(),
    override val date: LocalDate,
    override val type: MedicalRecordType,
    override val description: String?,
    override val notes: String?,
    override val tags: List<String> = emptyList(),
    val testName: String,
    val value: Double?,
    val unit: String?,
) : MedicalRecord(id, date, type, description, notes, tags)

fun MeasurementRecord.toMedicalRecordEntity(medicalRecordId: Uuid): MedicalRecordEntity {
    return MedicalRecordEntity(
        id = medicalRecordId,
        date = this.date,
        type = this.type,
        description = this.description,
        notes = this.notes,
    )
}

fun MeasurementRecord.toMeasurementRecordEntity(medicalRecordId: Uuid): MeasurementRecordEntity {
    return MeasurementRecordEntity(
        id = this.id,
        medicalRecordId = medicalRecordId,
        testName = this.testName,
        value = this.value,
        unit = this.unit,
    )
}
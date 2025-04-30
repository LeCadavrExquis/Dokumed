package pl.fzar.dokumed.data.model

import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.entity.ConsultationRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import kotlin.uuid.Uuid

data class ConsultationRecord(
    override val id: Uuid = Uuid.random(),
    override val date: LocalDate,
    override val type: MedicalRecordType,
    override val description: String?,
    override val notes: String?,
    override val tags: List<String> = emptyList(),
    val filePath: String? = null,
    val fileMimeType: String? = null,
    val doctor: String?
) : MedicalRecord(id, date, type, description, notes, tags)

fun ConsultationRecord.toMedicalRecordEntity(medicalRecordId: Uuid): MedicalRecordEntity {
    return MedicalRecordEntity(
        id = medicalRecordId,
        date = this.date,
        type = this.type,
        description = this.description,
        notes = this.notes,
    )
}

fun ConsultationRecord.toConsultationRecordEntity(medicalRecordId: Uuid): ConsultationRecordEntity {
    return ConsultationRecordEntity(
        id = this.id,
        medicalRecordId = medicalRecordId,
        filePath = this.filePath,
        fileMimeType = this.fileMimeType,
        doctor = this.doctor,
    )
}
package pl.fzar.dokumed.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.model.ClinicalDataRecord
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
import pl.fzar.dokumed.data.model.MedicalRecordType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(tableName = "medical_records")
data class MedicalRecordEntity @OptIn(ExperimentalUuidApi::class) constructor(
    @PrimaryKey
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val type: MedicalRecordType,
    val description: String?,
    val notes: String?,
)

data class MedicalRecordWithTags(
    @Embedded val medicalRecord: MedicalRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MedicalRecordTagCrossRef::class,
            parentColumn = "medicalRecordId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)

fun MedicalRecordWithTags.toConsultationRecord(): ConsultationRecord {
    return ConsultationRecord(
        id = this.medicalRecord.id,
        date = this.medicalRecord.date,
        type = this.medicalRecord.type,
        description = this.medicalRecord.description,
        notes = this.medicalRecord.notes,
        tags = this.tags.map { it.name },
        filePath = null,
        fileMimeType = null,
        doctor = null,
    )
}

fun MedicalRecordWithTags.toMeasurementRecord(): MeasurementRecord {
    return MeasurementRecord(
        id = this.medicalRecord.id,
        date = this.medicalRecord.date,
        type = this.medicalRecord.type,
        description = this.medicalRecord.description,
        notes = this.medicalRecord.notes,
        tags = this.tags.map { it.name },
        testName = "",
        value = null,
        unit = null,
    )
}

fun MedicalRecordWithTags.toClinicalDataRecord(): ClinicalDataRecord {
    return ClinicalDataRecord(
        id = this.medicalRecord.id,
        date = this.medicalRecord.date,
        type = this.medicalRecord.type,
        description = this.medicalRecord.description,
        notes = this.medicalRecord.notes,
        tags = this.tags.map { it.name },
        filePath = null,
        fileMimeType = null,
        testName = "",
    )
}
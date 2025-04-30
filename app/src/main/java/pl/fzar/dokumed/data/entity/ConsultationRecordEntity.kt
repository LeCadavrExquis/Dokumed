package pl.fzar.dokumed.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import pl.fzar.dokumed.data.model.ConsultationRecord
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "consultation_records",
    foreignKeys = [ForeignKey(
        entity = MedicalRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicalRecordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicalRecordId")]
)
data class ConsultationRecordEntity @OptIn(ExperimentalUuidApi::class) constructor(
    @PrimaryKey
    val id: Uuid = Uuid.random(),
    val medicalRecordId: Uuid,
    val filePath: String?,
    val fileMimeType: String?,
    val doctor: String?
)

data class ConsultationRecordWithDetails(
    @Embedded val consultation: ConsultationRecordEntity,
    @Relation(
        parentColumn = "medicalRecordId",
        entityColumn = "id"
    )
    val medicalRecord: MedicalRecordEntity,
    @Relation(
        parentColumn = "medicalRecordId",
        entityColumn = "id",
        associateBy = Junction(
            value = MedicalRecordTagCrossRef::class,
            parentColumn = "medicalRecordId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)

fun ConsultationRecordWithDetails.toConsultationRecord(): ConsultationRecord {
    return ConsultationRecord(
        id = consultation.id,
        date = medicalRecord.date,
        type = medicalRecord.type,
        description = medicalRecord.description,
        notes = medicalRecord.notes,
        tags = tags.map { it.name },
        filePath = consultation.filePath,
        fileMimeType = consultation.fileMimeType,
        doctor = consultation.doctor,
    )
}
package pl.fzar.dokumed.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import pl.fzar.dokumed.data.model.ConsultationRecord
import pl.fzar.dokumed.data.model.MeasurementRecord
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "measurement_records",
    foreignKeys = [ForeignKey(
        entity = MedicalRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicalRecordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicalRecordId")]
)
data class MeasurementRecordEntity @OptIn(ExperimentalUuidApi::class) constructor(
    @PrimaryKey
    val id: Uuid = Uuid.random(),
    val medicalRecordId: Uuid,
    val testName: String,
    val value: Double?,
    val unit: String?
)

data class MeasurementRecordWithDetails(
    @Embedded val measurement: MeasurementRecordEntity,
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

fun MeasurementRecordWithDetails.toMeasurementRecord(): MeasurementRecord {
    return MeasurementRecord(
        id = measurement.id,
        date = medicalRecord.date,
        type = medicalRecord.type,
        description = medicalRecord.description,
        notes = medicalRecord.notes,
        tags = tags.map { it.name },
        testName = measurement.testName,
        value = measurement.value,
        unit = measurement.unit,
    )
}
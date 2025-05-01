package pl.fzar.dokumed.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.model.MedicalRecordType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(tableName = "medical_records")
data class MedicalRecordEntity @OptIn(ExperimentalUuidApi::class) constructor(
    @PrimaryKey
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val type: MedicalRecordType,
    val doctor: String?,
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

// New data class to hold the medical record with all its details
data class MedicalRecordWithDetails(
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
    val tags: List<TagEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "medicalRecordId"
    )
    val measurements: List<MeasurementEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "medicalRecordId"
    )
    val clinicalData: List<ClinicalDataEntity> = emptyList()
)
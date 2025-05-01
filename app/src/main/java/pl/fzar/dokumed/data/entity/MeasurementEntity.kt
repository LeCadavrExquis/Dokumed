package pl.fzar.dokumed.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid

@Entity(
    tableName = "measurements",
    foreignKeys = [ForeignKey(
        entity = MedicalRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicalRecordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicalRecordId")]
)
data class MeasurementEntity(
    @PrimaryKey
    val id: Uuid,
    val medicalRecordId: Uuid,
    val value: Double?,
    val unit: String?
)
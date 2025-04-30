package pl.fzar.dokumed.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "medical_record_tag_cross_ref",
    primaryKeys = ["medicalRecordId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = MedicalRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicalRecordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("medicalRecordId"),
        Index("tagId")
    ]
)
data class MedicalRecordTagCrossRef(
    val medicalRecordId: Uuid,
    val tagId: Long
)
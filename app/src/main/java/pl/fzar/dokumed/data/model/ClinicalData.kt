package pl.fzar.dokumed.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid

@Entity(
    tableName = "clinical_data",
    foreignKeys = [ForeignKey(
        entity = MedicalRecord::class,
        parentColumns = ["id"],
        childColumns = ["recordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["recordId"])]
)
data class ClinicalData(
    @PrimaryKey val id: Uuid = Uuid.random(),
    val recordId: Uuid? = null, // Link to MedicalRecord
    val filePath: String? = null,
    val fileMimeType: String? = null,
    val fileName: String? = null, // Added field for original filename
)
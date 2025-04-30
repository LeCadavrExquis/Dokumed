package pl.fzar.dokumed.data.dao

import androidx.room.*
import pl.fzar.dokumed.data.entity.MedicalRecordEntity
import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.TagEntity
import kotlin.uuid.Uuid

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: MedicalRecordTagCrossRef)

    @Transaction
    @Query("""
        SELECT * FROM tags
        INNER JOIN medical_record_tag_cross_ref ON tags.id = medical_record_tag_cross_ref.tagId
        WHERE medical_record_tag_cross_ref.medicalRecordId = :medicalRecordId
    """)
    suspend fun getTagsForMedicalRecord(medicalRecordId: Uuid): List<TagEntity>

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Delete
    suspend fun deleteCrossRef(crossRef: MedicalRecordTagCrossRef)

    @Query("SELECT * FROM tags WHERE name = :tagName LIMIT 1")
    suspend fun getTagByName(tagName: String): TagEntity?

    @Query("DELETE FROM medical_record_tag_cross_ref WHERE medicalRecordId = :medicalRecordId")
    suspend fun deleteCrossRefsForMedicalRecord(medicalRecordId: Uuid)

    @Query("SELECT * FROM tags ORDER BY name")
    suspend fun getAllTags(): List<TagEntity>
}
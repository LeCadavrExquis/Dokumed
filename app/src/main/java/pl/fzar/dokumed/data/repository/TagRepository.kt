package pl.fzar.dokumed.data.repository

import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.TagEntity
import kotlin.uuid.Uuid

/**
 * Repository interface for tag operations
 */
interface TagRepository {
    /**
     * Get all tags for a medical record
     */
    suspend fun getTagsForMedicalRecord(medicalRecordId: Uuid): List<TagEntity>
    
    /**
     * Get a tag by name
     */
    suspend fun getTagByName(tagName: String): TagEntity?
    
    /**
     * Insert a new tag
     */
    suspend fun insertTag(tag: TagEntity): Long
    
    /**
     * Insert a reference between a medical record and a tag
     */
    suspend fun insertCrossRef(crossRef: MedicalRecordTagCrossRef)
    
    /**
     * Update an existing tag
     */
    suspend fun updateTag(tag: TagEntity)
    
    /**
     * Delete a tag
     */
    suspend fun deleteTag(tag: TagEntity)
    
    /**
     * Delete a reference between a medical record and a tag
     */
    suspend fun deleteCrossRef(crossRef: MedicalRecordTagCrossRef)
    
    /**
     * Delete all tag references for a medical record
     */
    suspend fun deleteCrossRefsForMedicalRecord(medicalRecordId: Uuid)
    
    /**
     * Get all available tags
     */
    suspend fun getAllTags(): List<TagEntity>
}

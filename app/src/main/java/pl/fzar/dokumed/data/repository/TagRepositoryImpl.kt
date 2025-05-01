package pl.fzar.dokumed.data.repository

import kotlinx.coroutines.flow.Flow
import pl.fzar.dokumed.data.dao.TagDao
import pl.fzar.dokumed.data.entity.MedicalRecordTagCrossRef
import pl.fzar.dokumed.data.entity.TagEntity
import kotlin.uuid.Uuid

/**
 * Implementation of the TagRepository interface
 */
class TagRepositoryImpl(
    private val tagDao: TagDao
) : TagRepository {

    override suspend fun getTagsForMedicalRecord(medicalRecordId: Uuid): List<TagEntity> {
        return tagDao.getTagsForMedicalRecord(medicalRecordId)
    }

    override suspend fun getTagByName(tagName: String): TagEntity? {
        return tagDao.getTagByName(tagName)
    }

    override suspend fun insertTag(tag: TagEntity): Long {
        return tagDao.insertTag(tag)
    }

    override suspend fun insertCrossRef(crossRef: MedicalRecordTagCrossRef) {
        tagDao.insertCrossRef(crossRef)
    }

    override suspend fun updateTag(tag: TagEntity) {
        tagDao.updateTag(tag)
    }

    override suspend fun deleteTag(tag: TagEntity) {
        tagDao.deleteTag(tag)
    }

    override suspend fun deleteCrossRef(crossRef: MedicalRecordTagCrossRef) {
        tagDao.deleteCrossRef(crossRef)
    }

    override suspend fun deleteCrossRefsForMedicalRecord(medicalRecordId: Uuid) {
        tagDao.deleteCrossRefsForMedicalRecord(medicalRecordId)
    }
    
    override suspend fun getAllTags(): List<TagEntity> {
        return tagDao.getAllTags()
    }
}
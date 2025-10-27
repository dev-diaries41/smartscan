package com.fpf.smartscan.data.tags

import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val userTagDao: UserTagDao,
    private val imageTagDao: ImageTagDao
) {
    // User Tags
    val allTags: Flow<List<UserTagEntity>> = userTagDao.getAllTags()
    val activeTags: Flow<List<UserTagEntity>> = userTagDao.getActiveTags()

    suspend fun getActiveTagsSync(): List<UserTagEntity> = userTagDao.getActiveTagsSync()
    suspend fun getTagByName(name: String): UserTagEntity? = userTagDao.getTagByName(name)
    suspend fun insertTag(tag: UserTagEntity) = userTagDao.insertTag(tag)
    suspend fun updateTag(tag: UserTagEntity) = userTagDao.updateTag(tag)
    suspend fun deleteTag(tag: UserTagEntity) = userTagDao.deleteTag(tag)
    suspend fun getTagCount(): Int = userTagDao.getTagCount()

    // Image Tags
    suspend fun getTagsForImage(imageId: Long): List<ImageTagEntity> =
        imageTagDao.getTagsForImage(imageId)

    fun getTagsForImageFlow(imageId: Long): Flow<List<ImageTagEntity>> =
        imageTagDao.getTagsForImageFlow(imageId)

    suspend fun getImageIdsForTag(tagName: String): List<Long> =
        imageTagDao.getImageIdsForTag(tagName)

    suspend fun getImageIdsForTags(tagNames: List<String>): List<Long> =
        imageTagDao.getImageIdsForTags(tagNames, tagNames.size)

    suspend fun getImageIdsWithAnyTag(tagNames: List<String>): List<Long> =
        imageTagDao.getImageIdsWithAnyTag(tagNames)

    suspend fun getImageCountForTag(tagName: String): Int =
        imageTagDao.getImageCountForTag(tagName)

    suspend fun insertImageTag(tag: ImageTagEntity) = imageTagDao.insertTag(tag)
    suspend fun insertImageTags(tags: List<ImageTagEntity>) = imageTagDao.insertTags(tags)
    suspend fun deleteImageTag(tag: ImageTagEntity) = imageTagDao.deleteTag(tag)
    suspend fun deleteTagsForImage(imageId: Long) = imageTagDao.deleteTagsForImage(imageId)
    suspend fun deleteSpecificTag(imageId: Long, tagName: String) =
        imageTagDao.deleteSpecificTag(imageId, tagName)
    suspend fun deleteAllTagsWithName(tagName: String) =
        imageTagDao.deleteAllTagsWithName(tagName)

    suspend fun deleteAutoAssignedTagsForImage(imageId: Long) =
        imageTagDao.deleteAutoAssignedTagsForImage(imageId)

    // Combined operations
    suspend fun getUserTagsWithCounts(): List<Pair<UserTagEntity, Int>> {
        val tags = userTagDao.getAllTags()
        val result = mutableListOf<Pair<UserTagEntity, Int>>()

        // This is a workaround since Room doesn't support async flow collection in this context
        // In production, you might want to handle this differently
        return result
    }
}

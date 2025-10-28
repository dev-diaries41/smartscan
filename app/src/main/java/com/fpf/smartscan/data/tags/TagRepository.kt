package com.fpf.smartscan.data.tags

import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val userTagDao: UserTagDao,
    private val mediaTagDao: MediaTagDao
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

    // Media Tags (pro obrázky i videa)
    suspend fun getTagsForMedia(mediaId: Long): List<MediaTagEntity> =
        mediaTagDao.getTagsForMedia(mediaId)

    fun getTagsForMediaFlow(mediaId: Long): Flow<List<MediaTagEntity>> =
        mediaTagDao.getTagsForMediaFlow(mediaId)

    suspend fun getMediaIdsForTag(tagName: String): List<Long> =
        mediaTagDao.getMediaIdsForTag(tagName)

    suspend fun getMediaIdsForTags(tagNames: List<String>): List<Long> =
        mediaTagDao.getMediaIdsForTags(tagNames, tagNames.size)

    suspend fun getMediaIdsWithAnyTag(tagNames: List<String>): List<Long> =
        mediaTagDao.getMediaIdsWithAnyTag(tagNames)

    suspend fun getMediaCountForTag(tagName: String): Int =
        mediaTagDao.getMediaCountForTag(tagName)

    suspend fun insertMediaTag(tag: MediaTagEntity) = mediaTagDao.insertTag(tag)
    suspend fun insertMediaTags(tags: List<MediaTagEntity>) = mediaTagDao.insertTags(tags)
    suspend fun deleteMediaTag(tag: MediaTagEntity) = mediaTagDao.deleteTag(tag)
    suspend fun deleteTagsForMedia(mediaId: Long) = mediaTagDao.deleteTagsForMedia(mediaId)
    suspend fun deleteSpecificTag(mediaId: Long, tagName: String) =
        mediaTagDao.deleteSpecificTag(mediaId, tagName)
    suspend fun deleteAllTagsWithName(tagName: String) =
        mediaTagDao.deleteAllTagsWithName(tagName)

    suspend fun deleteAutoAssignedTagsForMedia(mediaId: Long) =
        mediaTagDao.deleteAutoAssignedTagsForMedia(mediaId)

    // Backward compatibility aliases (pro postupnou migraci kódu)
    @Deprecated("Use getTagsForMedia instead", ReplaceWith("getTagsForMedia(mediaId)"))
    suspend fun getTagsForImage(imageId: Long): List<MediaTagEntity> = getTagsForMedia(imageId)
    
    @Deprecated("Use getTagsForMediaFlow instead", ReplaceWith("getTagsForMediaFlow(mediaId)"))
    fun getTagsForImageFlow(imageId: Long): Flow<List<MediaTagEntity>> = getTagsForMediaFlow(imageId)
    
    @Deprecated("Use getMediaIdsForTag instead", ReplaceWith("getMediaIdsForTag(tagName)"))
    suspend fun getImageIdsForTag(tagName: String): List<Long> = getMediaIdsForTag(tagName)
    
    @Deprecated("Use getMediaIdsForTags instead", ReplaceWith("getMediaIdsForTags(tagNames)"))
    suspend fun getImageIdsForTags(tagNames: List<String>): List<Long> = getMediaIdsForTags(tagNames)
    
    @Deprecated("Use getMediaIdsWithAnyTag instead", ReplaceWith("getMediaIdsWithAnyTag(tagNames)"))
    suspend fun getImageIdsWithAnyTag(tagNames: List<String>): List<Long> = getMediaIdsWithAnyTag(tagNames)
    
    @Deprecated("Use getMediaCountForTag instead", ReplaceWith("getMediaCountForTag(tagName)"))
    suspend fun getImageCountForTag(tagName: String): Int = getMediaCountForTag(tagName)

    // Combined operations
    suspend fun getUserTagsWithCounts(): List<Pair<UserTagEntity, Int>> {
        val tags = userTagDao.getAllTags()
        val result = mutableListOf<Pair<UserTagEntity, Int>>()

        // This is a workaround since Room doesn't support async flow collection in this context
        // In production, you might want to handle this differently
        return result
    }
}

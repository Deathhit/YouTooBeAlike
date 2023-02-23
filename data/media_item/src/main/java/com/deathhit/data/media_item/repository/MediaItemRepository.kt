package com.deathhit.data.media_item.repository

import androidx.paging.PagingData
import com.deathhit.data.media_item.MediaItemDO
import com.deathhit.data.media_item.MediaItemSourceType
import kotlinx.coroutines.flow.Flow

interface MediaItemRepository {
    suspend fun clearAll(mediaItemSourceType: MediaItemSourceType)

    fun getMediaItemPagingDataFlow(
        exclusiveId: String? = null,
        mediaItemSourceType: MediaItemSourceType,
        subtitle: String? = null
    ): Flow<PagingData<MediaItemDO>>
}
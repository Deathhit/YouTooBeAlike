package com.deathhit.data.media_item

import androidx.paging.PagingData
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.data.media_item.model.MediaItemSourceType
import kotlinx.coroutines.flow.Flow

interface MediaItemRepository {
    suspend fun clearAll(mediaItemSourceType: MediaItemSourceType)

    fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?>

    fun getMediaItemPagingDataFlow(
        exclusiveId: String? = null,
        mediaItemSourceType: MediaItemSourceType,
        subtitle: String? = null
    ): Flow<PagingData<MediaItemDO>>
}
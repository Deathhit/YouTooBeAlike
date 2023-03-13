package com.deathhit.data.media_item

import androidx.paging.PagingData
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.data.media_item.model.MediaItemLabel
import kotlinx.coroutines.flow.Flow

interface MediaItemRepository {
    suspend fun clearByLabel(mediaItemLabel: MediaItemLabel)

    fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?>

    fun getMediaItemPagingDataFlow(
        exclusiveId: String? = null,
        mediaItemLabel: MediaItemLabel,
        subtitle: String? = null
    ): Flow<PagingData<MediaItemDO>>
}
package com.deathhit.data.media_item.repository

import androidx.paging.PagingData
import com.deathhit.data.media_item.MediaItemDO
import kotlinx.coroutines.flow.Flow

interface MediaItemRepository {
    fun getMediaItemPagingDataFlow(exclusiveId: String? = null, subtitle: String? = null): Flow<PagingData<MediaItemDO>>
}
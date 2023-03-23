package com.deathhit.domain

import androidx.paging.PagingData
import com.deathhit.domain.enum_type.MediaItemLabel
import com.deathhit.domain.model.MediaItemDO
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
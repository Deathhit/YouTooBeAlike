package com.deathhit.feature.media_item_list.config

import androidx.paging.PagingData
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.model.MediaItemDO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class FakeMediaItemRepository : MediaItemRepository {
    override suspend fun clearByLabel(mediaItemLabel: com.deathhit.domain.enum_type.MediaItemLabel) {

    }

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> = emptyFlow()

    override fun getMediaItemPagingDataFlow(
        exclusiveId: String?,
        mediaItemLabel: com.deathhit.domain.enum_type.MediaItemLabel,
        subtitle: String?
    ): Flow<PagingData<MediaItemDO>> = flowOf(PagingData.empty())
}
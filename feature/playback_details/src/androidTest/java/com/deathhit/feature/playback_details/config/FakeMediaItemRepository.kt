package com.deathhit.feature.playback_details.config

import androidx.paging.PagingData
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.enum_type.MediaItemLabel
import com.deathhit.domain.model.MediaItemDO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMediaItemRepository : MediaItemRepository {
    var isClearByLabelCalled = false
    var mediaItemDO: MediaItemDO? = null

    override suspend fun clearByLabel(mediaItemLabel: MediaItemLabel) {
        isClearByLabelCalled = true
    }

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> =
        flowOf(mediaItemDO)

    override fun getMediaItemPagingDataFlow(
        exclusiveId: String?,
        mediaItemLabel: MediaItemLabel,
        subtitle: String?
    ): Flow<PagingData<MediaItemDO>> = flowOf(PagingData.empty())
}
package com.deathhit.data.media_item.config

import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource

internal class TestMediaItemRemoteDataSource(private val mediaItemRemoteDataSource: MediaItemRemoteDataSource) : MediaItemRemoteDataSource {
    sealed interface Action {
        data class GetMediaList(val exclusiveId: String?, val page: Int, val pageSize: Int, val subtitle: String?) : Action
    }

    var actions = emptyList<Action>()

    override suspend fun getMediaList(
        exclusiveId: String?,
        page: Int,
        pageSize: Int,
        subtitle: String?
    ): List<Media> = mediaItemRemoteDataSource.getMediaList(exclusiveId, page, pageSize, subtitle).also {
        actions = actions + Action.GetMediaList(exclusiveId, page, pageSize, subtitle)
    }
}
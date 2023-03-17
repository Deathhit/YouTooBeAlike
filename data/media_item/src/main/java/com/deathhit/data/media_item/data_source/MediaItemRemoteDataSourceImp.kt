package com.deathhit.data.media_item.data_source

import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.model.Media

internal class MediaItemRemoteDataSourceImp(private val mediaApiService: MediaApiService) :
    MediaItemRemoteDataSource {
    override suspend fun getMediaList(
        exclusiveId: String?,
        page: Int,
        pageSize: Int,
        subtitle: String?
    ): List<Media> =
        mediaApiService.getMediaList(exclusiveId, page, pageSize, subtitle)
}
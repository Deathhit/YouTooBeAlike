package com.deathhit.data.media_item.data_source

import com.deathhit.core.media_api.MediaApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaItemRemoteDataSource @Inject constructor(private val mediaApiService: MediaApiService) {
    companion object {
        const val DEFAULT_PAGE = MediaApiService.DEFAULT_PAGE
    }

    suspend fun getMediaList(exclusiveId: String?, page: Int?, pageSize: Int, subtitle: String?) =
        mediaApiService.getMediaList(exclusiveId, page ?: 0, pageSize, subtitle)
}
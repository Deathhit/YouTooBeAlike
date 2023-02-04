package com.deathhit.data.media_item.data_source

import com.deathhit.core.media_api.service.MediaApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaItemRemoteDataSource @Inject constructor(private val mediaApiService: MediaApiService) {
    companion object {
        const val DEFAULT_PAGE = MediaApiService.DEFAULT_PAGE
    }

    suspend fun getMediaList(page: Int?, pageSize: Int) =
        mediaApiService.getMediaList(page ?: 0, pageSize)
}
package com.deathhit.core.media_api.service

import com.deathhit.core.media_api.model.Media

interface MediaApiService {
    companion object {
        const val DEFAULT_PAGE = 0
    }

    suspend fun getMediaList(page: Int?, pageSize: Int): List<Media>
}
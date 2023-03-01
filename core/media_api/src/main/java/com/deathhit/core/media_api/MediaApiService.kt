package com.deathhit.core.media_api

import com.deathhit.core.media_api.model.Media

interface MediaApiService {
    companion object {
        const val DEFAULT_PAGE = 0
    }

    suspend fun getMediaList(exclusiveId: String?, page: Int?, pageSize: Int, subtitle: String?): List<Media>
}
package com.deathhit.core.media_api

import com.deathhit.core.media_api.model.Media

interface MediaApiService {
    companion object {
        const val FIRST_PAGE = 0
    }

    suspend fun getMediaList(exclusiveId: String?, page: Int = FIRST_PAGE, pageSize: Int, subtitle: String?): List<Media>
}
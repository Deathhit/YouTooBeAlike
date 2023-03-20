package com.deathhit.data.media_item.data_source

import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.model.Media

internal interface MediaItemRemoteDataSource {
    companion object {
        const val FIRST_PAGE = MediaApiService.FIRST_PAGE
    }

    suspend fun getMediaList(exclusiveId: String?, page: Int, pageSize: Int, subtitle: String?): List<Media>
}
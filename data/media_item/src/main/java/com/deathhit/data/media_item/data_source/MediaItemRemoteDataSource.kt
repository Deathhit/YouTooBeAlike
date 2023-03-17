package com.deathhit.data.media_item.data_source

import com.deathhit.core.media_api.model.Media

interface MediaItemRemoteDataSource {
    suspend fun getMediaList(exclusiveId: String?, page: Int?, pageSize: Int, subtitle: String?): List<Media>
}
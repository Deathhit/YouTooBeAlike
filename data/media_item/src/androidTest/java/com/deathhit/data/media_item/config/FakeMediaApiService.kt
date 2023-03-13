package com.deathhit.data.media_item.config

import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.model.Media

class FakeMediaApiService : MediaApiService {
    val mutableMediaList = mutableListOf<Media>()

    var isThrowingError = false

    override suspend fun getMediaList(
        exclusiveId: String?,
        page: Int?,
        pageSize: Int,
        subtitle: String?
    ): List<Media> {
        if (isThrowingError) throw RuntimeException("isThrowingError == true")

        val offset = (page ?: MediaApiService.DEFAULT_PAGE) * pageSize
        val limit = offset + pageSize

        return with(mutableMediaList.filter {
            (exclusiveId == null || it.id != exclusiveId)
                    && (subtitle == null || it.subtitle == subtitle)
        }) {
            if (offset > lastIndex)
                emptyList()
            else
                subList(offset, if (limit > size) size else limit)
        }
    }
}
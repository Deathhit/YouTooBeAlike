package com.deathhit.data.media_item.config

import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.model.Media

class FakeMediaApiService : MediaApiService {
    data class GetMediaList(val exclusiveId: String?, val page: Int, val pageSize: Int, val subtitle: String?)

    private var _getMediaList: GetMediaList? = null
    val getMediaList get() = _getMediaList

    var isThrowingError = false
    var mediaList: List<Media> = mutableListOf()

    override suspend fun getMediaList(
        exclusiveId: String?,
        page: Int,
        pageSize: Int,
        subtitle: String?
    ): List<Media> {
        _getMediaList = GetMediaList(exclusiveId, page, pageSize, subtitle)

        if (isThrowingError) throw RuntimeException("isThrowingError == true")

        if (page < 0 || pageSize <= 0)
            return emptyList()

        val offset = page * pageSize
        val limit = offset + pageSize

        return with(mediaList.filter {
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
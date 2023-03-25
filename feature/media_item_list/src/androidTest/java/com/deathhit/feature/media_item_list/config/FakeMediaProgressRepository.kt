package com.deathhit.feature.media_item_list.config

import com.deathhit.domain.MediaProgressRepository
import com.deathhit.domain.model.MediaProgressDO

class FakeMediaProgressRepository : MediaProgressRepository {
    var mediaProgressDO: MediaProgressDO? = null

    override suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressDO? =
        mediaProgressDO

    override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) {
        this.mediaProgressDO = mediaProgressDO
    }
}
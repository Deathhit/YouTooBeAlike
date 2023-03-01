package com.deathhit.data.media_progress

import com.deathhit.data.media_progress.model.MediaProgressDO

interface MediaProgressRepository {
    suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressDO?

    suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO)
}
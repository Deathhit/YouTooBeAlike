package com.deathhit.domain

import com.deathhit.domain.model.MediaProgressDO

interface MediaProgressRepository {
    suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressDO?

    suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO)
}
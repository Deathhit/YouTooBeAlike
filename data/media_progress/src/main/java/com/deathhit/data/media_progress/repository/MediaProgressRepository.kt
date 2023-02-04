package com.deathhit.data.media_progress.repository

import com.deathhit.data.media_progress.MediaProgressDO

interface MediaProgressRepository {
    suspend fun getMediaProgressBySourceUrl(sourceUrl: String): MediaProgressDO?

    suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO)
}
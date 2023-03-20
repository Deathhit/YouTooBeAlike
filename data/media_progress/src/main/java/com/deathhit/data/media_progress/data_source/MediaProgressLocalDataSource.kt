package com.deathhit.data.media_progress.data_source

import com.deathhit.core.app_database.entity.MediaProgressEntity

interface MediaProgressLocalDataSource {
    suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressEntity?

    suspend fun setMediaProgress(mediaProgressEntity: MediaProgressEntity)
}
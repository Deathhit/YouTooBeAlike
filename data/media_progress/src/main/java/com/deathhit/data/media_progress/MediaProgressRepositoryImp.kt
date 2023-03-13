package com.deathhit.data.media_progress

import com.deathhit.data.media_progress.model.MediaProgressDO
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource

internal class MediaProgressRepositoryImp(private val mediaProgressLocalDataSource: MediaProgressLocalDataSource) :
    MediaProgressRepository {
    override suspend fun getMediaProgressByMediaItemId(mediaItemId: String) =
        mediaProgressLocalDataSource.getMediaProgressByMediaItemId(mediaItemId)?.toMediaProgressDO()

    override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) =
        mediaProgressLocalDataSource.setMediaProgress(mediaProgressDO.toMediaProgressEntity())
}
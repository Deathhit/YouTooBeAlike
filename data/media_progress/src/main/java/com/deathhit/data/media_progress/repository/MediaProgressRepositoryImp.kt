package com.deathhit.data.media_progress.repository

import com.deathhit.data.media_progress.MediaProgressDO
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource
import com.deathhit.data.media_progress.toDO
import com.deathhit.data.media_progress.toEntity

internal class MediaProgressRepositoryImp(private val mediaProgressLocalDataSource: MediaProgressLocalDataSource) :
    MediaProgressRepository {
    override suspend fun getMediaProgressBySourceUrl(sourceUrl: String) =
        mediaProgressLocalDataSource.getMediaProgressBySourceUrl(sourceUrl)?.toDO()

    override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) =
        mediaProgressLocalDataSource.setMediaProgress(mediaProgressDO.toEntity())
}
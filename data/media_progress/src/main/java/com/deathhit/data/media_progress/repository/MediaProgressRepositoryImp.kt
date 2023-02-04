package com.deathhit.data.media_progress.repository

import com.deathhit.core.database.model.MediaProgressEntity
import com.deathhit.data.media_progress.MediaProgressDO
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource

internal class MediaProgressRepositoryImp(private val mediaProgressLocalDataSource: MediaProgressLocalDataSource) :
    MediaProgressRepository {
    override suspend fun getMediaProgressBySourceUrl(sourceUrl: String) =
        mediaProgressLocalDataSource.getMediaProgressBySourceUrl(sourceUrl)?.toDO()

    override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) =
        mediaProgressLocalDataSource.setMediaProgress(mediaProgressDO.toEntity())

    private fun MediaProgressDO.toEntity() = MediaProgressEntity(null, position, sourceUrl)

    private fun MediaProgressEntity.toDO() = MediaProgressDO(position, sourceUrl)
}
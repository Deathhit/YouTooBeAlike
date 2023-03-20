package com.deathhit.data.media_progress.config

import com.deathhit.core.app_database.entity.MediaProgressEntity
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource

class TestMediaProgressLocalDataSource(private val mediaProgressLocalDataSource: MediaProgressLocalDataSource) :
    MediaProgressLocalDataSource {
    sealed interface Action {
        data class GetMediaProgressByMediaItemId(val mediaItemId: String) : Action
        data class SetMediaProgress(val mediaProgressEntity: MediaProgressEntity) : Action
    }

    var actions = emptyList<Action>()

    override suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressEntity? =
        mediaProgressLocalDataSource.getMediaProgressByMediaItemId(mediaItemId).also {
            actions = actions + Action.GetMediaProgressByMediaItemId(mediaItemId)
        }

    override suspend fun setMediaProgress(mediaProgressEntity: MediaProgressEntity) =
        mediaProgressLocalDataSource.setMediaProgress(mediaProgressEntity).also {
            actions = actions + Action.SetMediaProgress(mediaProgressEntity)
        }
}
package com.deathhit.data.media_progress.data_source

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.entity.MediaProgressEntity

internal class MediaProgressLocalDataSourceImp(private val appDatabase: AppDatabase) : MediaProgressLocalDataSource {
    override suspend fun getMediaProgressByMediaItemId(mediaItemId: String) =
        appDatabase.mediaProgressDao().getByMediaItemId(mediaItemId)

    override suspend fun setMediaProgress(mediaProgressEntity: MediaProgressEntity) =
        appDatabase.mediaProgressDao().upsert(mediaProgressEntity)
}
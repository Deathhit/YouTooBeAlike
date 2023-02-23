package com.deathhit.data.media_progress.data_source

import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.model.MediaProgressEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaProgressLocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    suspend fun getMediaProgressByMediaItemId(mediaItemId: String) =
        appDatabase.mediaProgressDao().getByMediaItemId(mediaItemId)

    suspend fun setMediaProgress(mediaProgressEntity: MediaProgressEntity) =
        appDatabase.mediaProgressDao().upsert(mediaProgressEntity)
}
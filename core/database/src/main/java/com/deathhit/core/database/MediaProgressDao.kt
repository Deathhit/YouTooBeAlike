package com.deathhit.core.database

import androidx.room.*
import com.deathhit.core.database.entity.MediaProgressEntity

@Dao
interface MediaProgressDao {
    @Query("SELECT * FROM MediaProgressEntity WHERE MediaProgressEntity.${Column.MEDIA_ITEM_ID} = :mediaItemId")
    suspend fun getByMediaItemId(mediaItemId: String): MediaProgressEntity?

    @Upsert
    suspend fun upsert(entity: MediaProgressEntity)
}
package com.deathhit.core.app_database

import androidx.room.*
import com.deathhit.core.app_database.entity.RemoteKeysEntity

@Dao
interface RemoteKeysDao {
    @Query("DELETE FROM RemoteKeysEntity WHERE :label IS NULL OR :label = ${Column.LABEL}")
    suspend fun clearByLabel(label: String?)

    @Query("SELECT * FROM RemoteKeysEntity WHERE :label = ${Column.LABEL} AND :mediaItemId = ${Column.MEDIA_ITEM_ID}")
    suspend fun getByLabelAndMediaItemId(label: String, mediaItemId: String): RemoteKeysEntity?

    @Upsert
    suspend fun upsert(entities: List<RemoteKeysEntity>)
}
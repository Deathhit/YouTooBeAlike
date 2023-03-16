package com.deathhit.core.database

import androidx.paging.PagingSource
import androidx.room.*
import com.deathhit.core.database.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("DELETE FROM MediaItemEntity WHERE :label IS NULL OR ${Column.LABEL} == :label")
    suspend fun clearByLabel(label: String?)

    @Query("SELECT * FROM MediaItemEntity WHERE ${Column.MEDIA_ITEM_ID} == :mediaItemId")
    fun getFlowById(mediaItemId: String): Flow<MediaItemEntity?>

    @Query("SELECT * FROM MediaItemEntity WHERE :label IS NULL OR ${Column.LABEL} == :label")
    fun getPagingSource(label: String?): PagingSource<Int, MediaItemEntity>

    @Upsert
    suspend fun upsert(entities: List<MediaItemEntity>)
}
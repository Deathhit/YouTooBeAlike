package com.deathhit.core.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.deathhit.core.database.Column
import com.deathhit.core.database.model.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query(
        "DELETE FROM MediaItemEntity " +
                "WHERE ${Column.MEDIA_ITEM_SOURCE_TYPE} == :mediaItemSourceType"
    )
    suspend fun clearAll(mediaItemSourceType: String)

    @Query("SELECT * FROM MediaItemEntity WHERE ${Column.MEDIA_ITEM_ID} == :mediaItemId")
    fun getFlowById(mediaItemId: String): Flow<MediaItemEntity?>

    @Query(
        "SELECT * FROM MediaItemEntity " +
                "WHERE ${Column.MEDIA_ITEM_SOURCE_TYPE} == :mediaItemSourceType"
    )
    fun getPagingSource(mediaItemSourceType: String): PagingSource<Int, MediaItemEntity>

    @Upsert
    suspend fun upsert(entities: List<MediaItemEntity>)
}
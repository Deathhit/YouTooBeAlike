package com.deathhit.core.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.deathhit.core.database.Column
import com.deathhit.core.database.model.MediaItemEntity

@Dao
interface MediaItemDao {
    @Query("DELETE FROM MediaItemEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM MediaItemEntity " +
            "WHERE (:exclusiveId IS NULL OR ${Column.MEDIA_ITEM_ID} != :exclusiveId) " +
            "AND (:subtitle IS NULL OR ${Column.SUBTITLE} == :subtitle)")
    fun getPagingSource(exclusiveId: String?, subtitle: String?): PagingSource<Int, MediaItemEntity>

    @Upsert
    suspend fun upsert(entities: List<MediaItemEntity>)
}
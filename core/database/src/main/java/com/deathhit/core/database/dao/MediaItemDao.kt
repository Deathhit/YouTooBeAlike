package com.deathhit.core.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.deathhit.core.database.model.MediaItemEntity

@Dao
interface MediaItemDao {
    @Query("DELETE FROM MediaItemEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM MediaItemEntity")
    fun getPagingSource(): PagingSource<Int, MediaItemEntity>

    @Upsert
    suspend fun upsert(entities: List<MediaItemEntity>)
}
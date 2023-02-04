package com.deathhit.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deathhit.core.database.model.MediaItemEntity

@Dao
interface MediaItemDao {
    @Query("DELETE FROM MediaItemEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM MediaItemEntity")
    fun getPagingSource(): PagingSource<Int, MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(entities: List<MediaItemEntity>)
}
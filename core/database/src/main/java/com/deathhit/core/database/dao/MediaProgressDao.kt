package com.deathhit.core.database.dao

import androidx.room.*
import com.deathhit.core.database.Column
import com.deathhit.core.database.model.MediaProgressEntity

@Dao
interface MediaProgressDao {
    @Query("DELETE FROM MediaProgressEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM MediaProgressEntity WHERE MediaProgressEntity.${Column.SOURCE_URL} = :sourceUrl")
    suspend fun getBySourceUrl(sourceUrl: String): MediaProgressEntity?

    @Upsert
    suspend fun upsert(entity: MediaProgressEntity)
}
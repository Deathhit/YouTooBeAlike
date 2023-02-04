package com.deathhit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deathhit.core.database.Column
import com.deathhit.core.database.model.MediaProgressEntity

@Dao
interface MediaProgressDao {
    @Query("SELECT * FROM MediaProgressEntity WHERE MediaProgressEntity.${Column.SOURCE_URL} = :sourceUrl")
    suspend fun getBySourceUrl(sourceUrl: String): MediaProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: MediaProgressEntity)
}
package com.deathhit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deathhit.core.database.model.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Query("DELETE FROM RemoteKeyEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM RemoteKeyEntity WHERE :label = label")
    @Transaction
    suspend fun getByLabel(label: String): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: RemoteKeyEntity)
}
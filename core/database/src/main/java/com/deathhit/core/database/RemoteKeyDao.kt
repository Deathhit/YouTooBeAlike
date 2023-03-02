package com.deathhit.core.database

import androidx.room.*
import com.deathhit.core.database.model.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Query("DELETE FROM RemoteKeyEntity WHERE :label IS NULL OR :label = ${Column.LABEL}")
    suspend fun clearAll(label: String?)

    @Query("SELECT * FROM RemoteKeyEntity WHERE :label = ${Column.LABEL}")
    @Transaction
    suspend fun getByLabel(label: String): RemoteKeyEntity?

    @Upsert
    suspend fun upsert(entity: RemoteKeyEntity)
}
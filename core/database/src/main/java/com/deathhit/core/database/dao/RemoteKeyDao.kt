package com.deathhit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deathhit.core.database.Column
import com.deathhit.core.database.model.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Query("DELETE FROM RemoteKeyEntity WHERE :label = ${Column.LABEL}")
    suspend fun clearAll(label: String)

    @Query("SELECT * FROM RemoteKeyEntity WHERE :label = ${Column.LABEL}")
    @Transaction
    suspend fun getByLabel(label: String): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: RemoteKeyEntity)
}
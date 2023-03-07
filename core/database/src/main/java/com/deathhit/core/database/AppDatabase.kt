package com.deathhit.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.deathhit.core.database.entity.*

@Database(
    entities = [MediaItemEntity::class, MediaProgressEntity::class, RemoteKeyEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val FILE_NAME = "database_c41c06dbd21355aaa41fd4a8565bbd4c"

        fun create(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME).build()

        fun createInMemory(context: Context) =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun mediaProgressDao(): MediaProgressDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
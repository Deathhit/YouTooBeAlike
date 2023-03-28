package com.deathhit.data.media_progress

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.MediaProgressDao
import com.deathhit.core.app_database.entity.MediaProgressEntity
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MediaProgressLocalDataSourceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var appDatabase: AppDatabase

    @Inject
    internal lateinit var mediaProgressLocalDataSource: MediaProgressLocalDataSource

    private lateinit var mediaProgressDao: MediaProgressDao

    @Before
    fun before() {
        hiltRule.inject()

        mediaProgressDao = appDatabase.mediaProgressDao()
    }

    @After
    fun after() {
        appDatabase.close()
    }

    @Test
    fun getMediaProgressByMediaItemId_withDataInDatabase_returnEntity() = runTest {
        //Given
        val mediaProgress = MediaProgressEntity(isEnded = Random.nextBoolean(), mediaItemId = "mediaItemId", position = Random.nextLong())

        mediaProgressDao.upsert(mediaProgress)

        //When
        val result = mediaProgressLocalDataSource.getMediaProgressByMediaItemId(mediaProgress.mediaItemId)

        //Then
        assert(result == mediaProgress)
    }

    @Test
    fun getMediaProgressByMediaItemId_withoutDataInDatabase_returnNull() = runTest {
        //Given
        val mediaProgress = MediaProgressEntity(Random.nextBoolean(), "mediaItemId", Random.nextLong())

        //When
        val result = mediaProgressLocalDataSource.getMediaProgressByMediaItemId(mediaProgress.mediaItemId)

        //Then
        assert(result == null)
    }

    @Test
    fun setMediaProgress_initialState_insertEntity() = runTest {
        //Given
        val mediaProgress = MediaProgressEntity(Random.nextBoolean(), "mediaItemId", Random.nextLong())

        //When
        mediaProgressLocalDataSource.setMediaProgress(mediaProgress)

        //Then
        assert(mediaProgressDao.getByMediaItemId(mediaProgress.mediaItemId) == mediaProgress)
    }
}
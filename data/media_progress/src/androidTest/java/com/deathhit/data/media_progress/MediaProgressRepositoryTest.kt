package com.deathhit.data.media_progress

import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.MediaProgressDao
import com.deathhit.core.database.entity.MediaProgressEntity
import com.deathhit.data.media_progress.model.MediaProgressDO
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
class MediaProgressRepositoryTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var appDatabase: AppDatabase

    @Inject
    internal lateinit var mediaProgressRepository: MediaProgressRepository

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
    fun getMediaProgressByMediaItemIdShouldReturnExpectedResult() = runTest {
        //Given
        val mediaItemId0 = "0"
        val mediaItemId1 = "1"
        val mediaItemId2 = "2"

        val mediaProgressEntity0 = MediaProgressEntity(
            isEnded = Random.nextBoolean(),
            mediaItemId = mediaItemId0,
            position = Random.nextLong()
        )
        val mediaProgressEntity1 = MediaProgressEntity(
            isEnded = Random.nextBoolean(),
            mediaItemId = mediaItemId1,
            position = Random.nextLong()
        )

        with(mediaProgressDao) {
            upsert(mediaProgressEntity0)
            upsert(mediaProgressEntity1)
        }

        //When
        val mediaProgressDO0 = mediaProgressRepository.getMediaProgressByMediaItemId(mediaItemId0)
        val mediaProgressDO1 = mediaProgressRepository.getMediaProgressByMediaItemId(mediaItemId1)
        val mediaProgressDO2 = mediaProgressRepository.getMediaProgressByMediaItemId(mediaItemId2)

        //Then
        assert(mediaProgressDO0 == mediaProgressEntity0.toMediaProgressDO())
        assert(mediaProgressDO1 == mediaProgressEntity1.toMediaProgressDO())
        assert(mediaProgressDO2 == null)
    }

    @Test
    fun setMediaProgressShouldInsertCorrespondEntityToDatabase() = runTest {
        //Given
        val mediaItemId0 = "0"
        val mediaItemId1 = "1"
        val mediaItemId2 = "2"

        val mediaProgressDO0 = MediaProgressDO(
            isEnded = Random.nextBoolean(),
            mediaItemId = mediaItemId0,
            position = Random.nextLong()
        )
        val mediaProgressDO1 = MediaProgressDO(
            isEnded = Random.nextBoolean(),
            mediaItemId = mediaItemId1,
            position = Random.nextLong()
        )

        //When
        with(mediaProgressRepository) {
            setMediaProgress(mediaProgressDO0)
            setMediaProgress(mediaProgressDO1)
        }

        //Then
        with(mediaProgressDao) {
            assert(mediaProgressDO0.toMediaProgressEntity() == getByMediaItemId(mediaItemId0))
            assert(mediaProgressDO1.toMediaProgressEntity() == getByMediaItemId(mediaItemId1))
            assert(null == getByMediaItemId(mediaItemId2))
        }
    }
}
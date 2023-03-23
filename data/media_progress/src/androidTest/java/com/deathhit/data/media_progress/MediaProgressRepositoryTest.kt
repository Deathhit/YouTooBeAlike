package com.deathhit.data.media_progress

import com.deathhit.data.media_progress.config.TestMediaProgressLocalDataSource
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.domain.MediaProgressRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    internal lateinit var mediaProgressRepository: MediaProgressRepository

    @Inject
    internal lateinit var testMediaProgressLocalDataSource: TestMediaProgressLocalDataSource

    @Before
    fun before() {
        hiltRule.inject()
    }

    @Test
    fun getMediaProgressByMediaItemId_initialState_invokeLocalDataSource() = runTest {
        //Given
        val mediaItemId = "mediaItemId"

        //When
        testMediaProgressLocalDataSource.getMediaProgressByMediaItemId(mediaItemId)

        //Then
        assert(
            testMediaProgressLocalDataSource.actions.contains(
                TestMediaProgressLocalDataSource.Action.GetMediaProgressByMediaItemId(
                    mediaItemId
                )
            )
        )
    }

    @Test
    fun setMediaProgress_initialState_invokeLocalDataSource() = runTest {
        //Given
        val mediaProgress = MediaProgressDO(Random.nextBoolean(), "mediaItemId", Random.nextLong())

        //When
        mediaProgressRepository.setMediaProgress(mediaProgress)

        //Then
        assert(
            testMediaProgressLocalDataSource.actions.contains(
                TestMediaProgressLocalDataSource.Action.SetMediaProgress(mediaProgress.toMediaProgressEntity())
            )
        )
    }
}
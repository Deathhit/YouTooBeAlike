package com.deathhit.data.media_item

import com.deathhit.data.media_item.config.TestMediaItemLocalDataSource
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.enum_type.MediaItemLabel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MediaItemRepositoryTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    internal lateinit var mediaItemRepository: MediaItemRepository

    @Inject
    internal lateinit var testMediaItemLocalDataSource: TestMediaItemLocalDataSource

    private lateinit var mediaItemLabel: MediaItemLabel

    @Before
    fun before() {
        hiltRule.inject()

        mediaItemLabel = with(MediaItemLabel.values()) {
            get(Random.nextInt(0, size))
        }
    }

    @Test
    fun clearByLabel_initialState_invokeClearByLabel() = runTest {
        //Given

        //When
        mediaItemRepository.clearByLabel(mediaItemLabel)

        //Then
        with(testMediaItemLocalDataSource.stateFlow.value) {
            assert(
                actions == listOf(
                    TestMediaItemLocalDataSource.State.Action.ClearByLabel(
                        mediaItemLabel.toLabelString()
                    )
                )
            )
        }
    }

    @Test
    fun getMediaItemFlowById_initialState_invokeGetMediaItemFlowById() = runTest {
        //Given
        val mediaItemId = "mediaItemId"

        //When
        mediaItemRepository.getMediaItemFlowById(mediaItemId)

        //Then
        with(testMediaItemLocalDataSource.stateFlow.value) {
            assert(
                actions == listOf(
                    TestMediaItemLocalDataSource.State.Action.GetMediaItemFlowById(
                        mediaItemId
                    )
                )
            )
        }
    }

    @Test
    fun getMediaItemPagingDataFlowFirst_initialState_invokeGetMediaItemPagingSource() = runTest {
        //Given

        //When
        mediaItemRepository.getMediaItemPagingDataFlow("exclusiveId", mediaItemLabel, "subtitle").first()

        advanceUntilIdle()

        //Then
        with(testMediaItemLocalDataSource.stateFlow.value) {
            assert(
                actions == listOf(
                    TestMediaItemLocalDataSource.State.Action.GetMediaItemPagingSource(
                        mediaItemLabel.toLabelString()
                    )
                )
            )
        }
    }
}
package com.deathhit.feature.playback_details

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.playback_details.model.toPlaybackDetailsVO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class PlaybackDetailsViewModelTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val mediaItemRepository = object : MediaItemRepository {
        var isClearByLabelCalled = false
        var mediaItemDO: MediaItemDO? = null

        override suspend fun clearByLabel(mediaItemLabel: com.deathhit.data.media_item.model.MediaItemLabel) {
            isClearByLabelCalled = true
        }

        override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> =
            flowOf(mediaItemDO)

        override fun getMediaItemPagingDataFlow(
            exclusiveId: String?,
            mediaItemLabel: com.deathhit.data.media_item.model.MediaItemLabel,
            subtitle: String?
        ): Flow<PagingData<MediaItemDO>> = flowOf(PagingData.empty())
    }

    private lateinit var viewModel: PlaybackDetailsViewModel

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModel = PlaybackDetailsViewModel(
            mediaItemRepository,
            SavedStateHandle.createHandle(null, Bundle.EMPTY)
        )

        runTest { advanceUntilIdle() }
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState() = runTest {
        //Given
        var collectedState: PlaybackDetailsViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When

        //Then
        collectedState?.run {
            assert(actions.isEmpty())
            assert(playbackDetails == null)
            assert(playItemId == null)
        }

        collectJob.cancel()
    }

    @Test
    fun onActionShouldReduceGivenAction() = runTest {
        //Given
        var collectedState: PlaybackDetailsViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        viewModel.retryLoadingRecommendedList()

        advanceUntilIdle()

        val action = collectedState!!.actions.last()

        //When
        viewModel.onAction(action)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(!actions.contains(action))
        }

        collectJob.cancel()
    }

    @Test
    fun openItemShouldAddCorrespondingActionWithGivenItem() = runTest {
        //Given
        var collectedState: PlaybackDetailsViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val mediaItem = MediaItemVO("id", "sourceUrl", "subtitle", "thumbUrl", "title")

        //When
        viewModel.openItem(mediaItem)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            val action = actions.last()

            assert(action is PlaybackDetailsViewModel.State.Action.OpenItem && action.item == mediaItem)
        }

        collectJob.cancel()
    }

    @Test
    fun retryLoadingRecommendedListShouldAddTheCorrespondingAction() = runTest {
        //Given
        var collectedState: PlaybackDetailsViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When
        viewModel.retryLoadingRecommendedList()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            val action = actions.last()

            assert(action is PlaybackDetailsViewModel.State.Action.RetryLoadingRecommendedList)
        }

        collectJob.cancel()
    }

    @Test
    fun setPlayItemIdShouldSetValueAndUpdatePlaybackDetails() = runTest {
        //Given
        var collectedState: PlaybackDetailsViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        //When
        viewModel.setPlayItemId(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(playbackDetails == mediaItemDO.toPlaybackDetailsVO())
            assert(playItemId == mediaItemDO.mediaItemId)
        }

        collectJob.cancel()
    }
}
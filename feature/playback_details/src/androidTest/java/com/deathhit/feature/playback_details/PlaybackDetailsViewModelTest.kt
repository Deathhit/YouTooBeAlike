package com.deathhit.feature.playback_details

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.playback_details.model.PlaybackDetailsVO
import com.deathhit.feature.playback_details.model.toPlaybackDetailsVO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class PlaybackDetailsViewModelTest {
    private class ViewModelBuilder(private val mediaItemRepository: MediaItemRepository) {
        sealed interface TestCase {
            object InitialState : TestCase
            data class PlayItemIdSet(val playItemId: String) : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = PlaybackDetailsViewModel(
            mediaItemRepository,
            SavedStateHandle.createHandle(null, Bundle.EMPTY)
        ).apply {
            when (val testCase = testCase) {
                is TestCase.PlayItemIdSet -> setPlayItemId(testCase.playItemId)
                else -> {}
            }

            runTest { advanceUntilIdle() }
        }
    }

    private class ViewModelStateAsserter(private val viewModel: PlaybackDetailsViewModel) {
        private val currentState get() = viewModel.stateFlow.value

        fun assertInitialState(
            playbackDetails: PlaybackDetailsVO? = null,
            playItemId: String? = null
        ) = with(currentState) {
            assert(actions.isEmpty())
            assert(this.playbackDetails == playbackDetails)
            assert(this.playItemId == playItemId)
        }

        fun assertActionsIsEmpty() {
            assert(currentState.actions.isEmpty())
        }

        fun assertLastActionIsOpenItem(item: MediaItemVO) =
            currentState.actions.last().let {
                assert(it is PlaybackDetailsViewModel.State.Action.OpenItem && it.item == item)
            }

        fun assertLastActionIsRetryLoadingRecommendedList() {
            assert(currentState.actions.last() is PlaybackDetailsViewModel.State.Action.RetryLoadingRecommendedList)
        }

        fun assertPlaybackDetails(playbackDetails: PlaybackDetailsVO?) {
            assert(currentState.playbackDetails == playbackDetails)
        }

        fun assertPlayItemId(playItemId: String?) {
            assert(currentState.playItemId == playItemId)
        }
    }

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

    private lateinit var viewModelBuilder: ViewModelBuilder

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModelBuilder = ViewModelBuilder(mediaItemRepository)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When

        //Then
        viewModelStateAsserter.assertInitialState()
    }

    @Test
    fun onAction_initialState_clearGivenAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        viewModel.retryLoadingRecommendedList()

        advanceUntilIdle()

        val action = viewModel.stateFlow.value.actions.last()

        //When
        viewModel.onAction(action)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertActionsIsEmpty()
    }

    @Test
    fun openItem_initialState_addAction() = runTest {
        //Given
        val mediaItem = MediaItemVO("id", "sourceUrl", "subtitle", "thumbUrl", "title")

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.openItem(mediaItem)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionIsOpenItem(mediaItem)
    }

    @Test
    fun retryLoadingRecommendedList_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.retryLoadingRecommendedList()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionIsRetryLoadingRecommendedList()
    }

    @Test
    fun setPlayItemId_initialState_setValueAndUpdatePlaybackDetails() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setPlayItemId(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        with(viewModelStateAsserter) {
            assertPlaybackDetails(mediaItemDO.toPlaybackDetailsVO())
            assertPlayItemId(mediaItemDO.mediaItemId)
        }
    }
}
package com.deathhit.feature.playback_details

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.enum_type.MediaItemLabel
import com.deathhit.domain.model.MediaItemDO
import com.deathhit.domain.test.FakeMediaItemRepository
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.playback_details.model.PlaybackDetailsVO
import com.deathhit.feature.playback_details.model.toPlaybackDetailsVO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class PlaybackDetailsViewModelTest {
    private class ViewModelBuilder(private val mediaItemRepository: MediaItemRepository) {
        sealed interface TestCase {
            object InitialState : TestCase
            data class PlaybackDetailsLoaded(val playItemId: String) : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = PlaybackDetailsViewModel(
            mediaItemRepository,
            SavedStateHandle.createHandle(null, Bundle.EMPTY)
        ).apply {
            runTest {
                when (val testCase = testCase) {
                    is TestCase.PlaybackDetailsLoaded -> loadPlaybackDetails(testCase.playItemId)
                    else -> {}
                }

                advanceUntilIdle()
            }
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

    @Inject
    lateinit var fakeMediaItemRepository: FakeMediaItemRepository

    private lateinit var viewModelBuilder: ViewModelBuilder

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModelBuilder = ViewModelBuilder(fakeMediaItemRepository)
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
    fun loadPlaybackDetails_initialState_setValueAndUpdatePlaybackDetails() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        fakeMediaItemRepository.funcGetMediaItemFlowById =
            { mediaItemId: String -> if (mediaItemId == mediaItemDO.mediaItemId) flowOf(mediaItemDO) else flowOf(null) }

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        val pagingDataCollectJob = launch {
            viewModel.recommendedItemPagingDataFlow.collect()
        }

        //When
        viewModel.loadPlaybackDetails(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        with(fakeMediaItemRepository.stateFlow.value) {
            val mediaItemLabel = MediaItemLabel.RECOMMENDED

            assert(
                actions == listOf(
                    FakeMediaItemRepository.State.Action.GetMediaItemFlowById(mediaItemId = mediaItemDO.mediaItemId),
                    FakeMediaItemRepository.State.Action.ClearByLabel(mediaItemLabel = mediaItemLabel),
                    FakeMediaItemRepository.State.Action.GetMediaItemPagingDataFlow(
                        exclusiveId = mediaItemDO.mediaItemId,
                        mediaItemLabel = mediaItemLabel,
                        subtitle = mediaItemDO.subtitle
                    )
                )
            )
        }

        with(viewModelStateAsserter) {
            assertPlaybackDetails(mediaItemDO.toPlaybackDetailsVO())
            assertPlayItemId(mediaItemDO.mediaItemId)
        }

        pagingDataCollectJob.cancel()
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
}
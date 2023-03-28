package com.deathhit.feature.playback_details

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.deathhit.domain.enum_type.MediaItemLabel
import com.deathhit.domain.model.MediaItemDO
import com.deathhit.domain.test.FakeMediaItemRepository
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.playback_details.model.toPlaybackDetailsVO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class PlaybackDetailsViewModelTest {
    private class ViewModelBuilder(private val fakeMediaItemRepository: FakeMediaItemRepository) {
        sealed interface TestCase {
            object InitialState : TestCase
            data class PlaybackDetailsLoaded(val mediaItemDO: MediaItemDO) : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = when (val testCase = testCase) {
            TestCase.InitialState -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    advanceUntilIdle()

                    with(stateFlow.value) {
                        assert(actions.isEmpty())
                        assert(playbackDetails == null)
                        assert(playItemId == null)
                    }
                }
            }
            is TestCase.PlaybackDetailsLoaded -> {
                //Given
                val mediaItemDO = testCase.mediaItemDO

                fakeMediaItemRepository.funcGetMediaItemFlowById =
                    { mediaItemId -> flowOf(if (mediaItemId == mediaItemDO.mediaItemId) mediaItemDO else null) }

                createViewModel().apply {
                    runTest {
                        //Given

                        //When
                        loadPlaybackDetails(mediaItemDO.mediaItemId)

                        advanceUntilIdle()

                        //Then
                        with(fakeMediaItemRepository.stateFlow.value) {
                            assert(
                                actions == listOf(
                                    FakeMediaItemRepository.State.Action.ClearByLabel(MediaItemLabel.RECOMMENDED),
                                    FakeMediaItemRepository.State.Action.GetMediaItemFlowById(
                                        mediaItemDO.mediaItemId
                                    )
                                )
                            )
                        }

                        with(stateFlow.value) {
                            assert(playbackDetails == mediaItemDO.toPlaybackDetailsVO())
                            assert(playItemId == mediaItemDO.mediaItemId)
                        }
                    }
                }
            }
        }

        private fun createViewModel() = PlaybackDetailsViewModel(
            fakeMediaItemRepository,
            SavedStateHandle.createHandle(null, Bundle.EMPTY)
        )
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
    fun loadPlaybackDetails_playbackDetailsLoaded_clearState() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlaybackDetailsLoaded(mediaItemDO)
        }.build()

        //When
        viewModel.loadPlaybackDetails(null)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        with(endState) {
            assert(playbackDetails == null)
            assert(playItemId == null)
        }
    }

    @Test
    fun loadPlaybackDetails_playbackDetailsLoaded_updateState() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlaybackDetailsLoaded(mediaItemDO)
        }.build()

        //When
        val newMediaItemDO = mediaItemDO.copy(mediaItemId = "newMediaItemId")

        fakeMediaItemRepository.funcGetMediaItemFlowById =
            { mediaItemId -> flowOf(if (mediaItemId == newMediaItemDO.mediaItemId) newMediaItemDO else null) }

        viewModel.loadPlaybackDetails(newMediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        with(endState) {
            assert(playbackDetails == newMediaItemDO.toPlaybackDetailsVO())
            assert(playItemId == newMediaItemDO.mediaItemId)
        }
    }

    @Test
    fun openItem_initialState_addAction() = runTest {
        //Given
        val mediaItemVO = MediaItemVO("id", "subtitle", "thumbUrl", "title")

        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.openItem(mediaItemVO)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(
            endState.actions.run {  subList(startState.actions.size, size) } == listOf(
                PlaybackDetailsViewModel.State.Action.OpenItem(
                    mediaItemVO
                )
            )
        )
    }

    @Test
    fun retryLoadingRecommendedList_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.retryLoadingRecommendedList()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(
            endState.actions.run {  subList(startState.actions.size, size) } == listOf(
                PlaybackDetailsViewModel.State.Action.RetryLoadingRecommendedList
            )
        )
    }
}
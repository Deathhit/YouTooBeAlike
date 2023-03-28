package com.deathhit.feature.navigation

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.deathhit.domain.model.MediaItemDO
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.domain.test.FakeMediaItemRepository
import com.deathhit.domain.test.FakeMediaProgressRepository
import com.deathhit.feature.media_item_list.model.toMediaItemVO
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
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class NavigationActivityViewModelTest {
    private class ViewModelBuilder(
        private val fakeMediaItemRepository: FakeMediaItemRepository,
        private val fakeMediaProgressRepository: FakeMediaProgressRepository
    ) {
        sealed interface TestCase {
            object Fullscreen : TestCase
            object InitialState : TestCase
            object PlayerConnected : TestCase
            data class PlayingByPlayerView(
                val mediaItemDO: MediaItemDO,
                val mediaProgressDO: MediaProgressDO
            ) : TestCase

            object RequestedScreenOrientationLandscape : TestCase
            object RequestedScreenOrientationPortrait : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = when (val testCase = testCase) {
            TestCase.Fullscreen -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    setIsPlayerViewExpanded(true)
                    setIsViewInLandscape(true)

                    advanceUntilIdle()

                    //Then
                    with(stateFlow.value) {
                        assert(isFullscreen)
                    }
                }
            }
            TestCase.InitialState -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    advanceUntilIdle()

                    //Then
                    with(stateFlow.value) {
                        assert(actions.isEmpty())
                        assert(attachedPages.isEmpty())
                        assert(currentPage == NavigationActivityViewModel.State.Page.HOME)
                        assert(!isFirstFrameRendered)
                        assert(!isPlayerConnected)
                        assert(!isPlayerViewExpanded)
                        assert(!isViewInForeground)
                        assert(!isViewInLandscape)
                        assert(playItem == null)
                        assert(playItemId == null)
                        assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.UNSPECIFIED)
                    }
                }
            }
            TestCase.PlayerConnected -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    setIsPlayerConnected(true)

                    //Then
                    with(stateFlow.value) {
                        assert(isPlayerConnected)
                    }
                }
            }
            is TestCase.PlayingByPlayerView -> {
                //Given
                val mediaItemDO = testCase.mediaItemDO
                val mediaProgressDO = testCase.mediaProgressDO

                fakeMediaItemRepository.funcGetMediaItemFlowById =
                    { mediaItemId -> flowOf(if (mediaItemId == mediaItemDO.mediaItemId) mediaItemDO else null) }

                fakeMediaProgressRepository.funcGetMediaProgressByMediaItemId =
                    { mediaItemId -> if (mediaItemId == mediaProgressDO.mediaItemId) mediaProgressDO else null }

                createViewModel().apply {
                    runTest {
                        //Given

                        //When
                        setIsPlayerConnected(true)

                        openItem(mediaItemDO.mediaItemId)

                        advanceUntilIdle()

                        //Then
                        with(fakeMediaItemRepository.stateFlow.value) {
                            assert(
                                actions == listOf(
                                    FakeMediaItemRepository.State.Action.GetMediaItemFlowById(
                                        mediaItemDO.mediaItemId
                                    )
                                )
                            )
                        }

                        with(fakeMediaProgressRepository.stateFlow.value) {
                            assert(
                                actions == listOf(
                                    FakeMediaProgressRepository.State.Action.GetMediaProgressByMediaItemId(
                                        mediaItemDO.mediaItemId
                                    )
                                )
                            )
                        }

                        with(stateFlow.value) {
                            assert(
                                actions == listOf(
                                    NavigationActivityViewModel.State.Action.ExpandPlayerView,
                                    NavigationActivityViewModel.State.Action.PlayPlayback,
                                    NavigationActivityViewModel.State.Action.StopPlayback,
                                    NavigationActivityViewModel.State.Action.PreparePlayback(
                                        mediaProgressDO.isEnded,
                                        mediaItemDO.mediaItemId,
                                        mediaProgressDO.position,
                                        mediaItemDO.sourceUrl
                                    )
                                )
                            )
                            assert(!isFirstFrameRendered)
                            assert(isPlayerConnected)
                            assert(isPlayingByPlayerView)
                            assert(playItem == mediaItemDO.toMediaItemVO())
                            assert(playItemId == mediaItemDO.mediaItemId)
                        }
                    }
                }
            }
            TestCase.RequestedScreenOrientationLandscape -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    toggleScreenOrientation()

                    advanceUntilIdle()

                    //Then
                    with(stateFlow.value) {
                        assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.LANDSCAPE)
                    }
                }
            }
            TestCase.RequestedScreenOrientationPortrait -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    setIsViewInLandscape(true)
                    toggleScreenOrientation()
                    setIsViewInLandscape(false)

                    advanceUntilIdle()

                    //Then
                    with(stateFlow.value) {
                        assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.PORTRAIT)
                    }
                }
            }
        }

        private fun createViewModel() = NavigationActivityViewModel(
            fakeMediaItemRepository,
            fakeMediaProgressRepository,
            SavedStateHandle.createHandle(null, Bundle.EMPTY)
        )
    }

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMediaItemRepository: FakeMediaItemRepository

    @Inject
    lateinit var fakeMediaProgressRepository: FakeMediaProgressRepository

    private lateinit var viewModelBuilder: ViewModelBuilder

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModelBuilder = ViewModelBuilder(
            fakeMediaItemRepository,
            fakeMediaProgressRepository
        )
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun clearPlayerViewPlayback_playingByPlayerView_clearState() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")
        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO, mediaProgressDO)
        }.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.clearPlayerViewPlayback()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        with(endState) {
            assert(!isFirstFrameRendered)
            assert(!isPlayingByPlayerView)
            assert(playItem == null)
            assert(playItemId == null)
        }

        assert(
            endState.actions.run { subList(startState.actions.size, size) } == listOf(
                NavigationActivityViewModel.State.Action.HidePlayerView,
                NavigationActivityViewModel.State.Action.StopPlayback,
            )
        )
    }

    @Test
    fun collapsePlayerView_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.collapsePlayerView()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(
            endState.actions.run { subList(startState.actions.size, size) } == listOf(
                NavigationActivityViewModel.State.Action.CollapsePlayerView
            )
        )
    }

    @Test
    fun collapsePlayerView_fullscreen_setOrientationToPortrait() = runTest {
        //Given
        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.Fullscreen }.build()

        //When
        viewModel.collapsePlayerView()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        with(endState) {
            assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.PORTRAIT)
        }
    }

    @Test
    fun notifyFirstFrameRendered_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.notifyFirstFrameRendered("mediaItemId")

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState == startState)
    }

    @Test
    fun notifyFirstFrameRendered_playingByPlayerView_setValueToTrue() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")
        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO, mediaProgressDO)
        }.build()

        //When
        viewModel.notifyFirstFrameRendered(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        with(endState) {
            assert(isFirstFrameRendered)
        }
    }

    @Test
    fun notifyPageAttached_initialState_addValue() = runTest {
        //Given
        val page = NavigationActivityViewModel.State.Page.values().run { get(Random.nextInt(size)) }

        val viewModel = viewModelBuilder.build()

        //When
        viewModel.notifyPageAttached(page)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.attachedPages.contains(page))
    }

    @Test
    fun openItem_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.openItem("mediaItemId")

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState == startState)
    }

    @Test
    fun pausePlayerViewPlayback_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.pausePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState == startState)
    }

    @Test
    fun pausePlayerViewPlayback_playingByPlayerView_addAction() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")
        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO, mediaProgressDO)
        }.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.pausePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.actions.run { subList(startState.actions.size, size) } == listOf(
            NavigationActivityViewModel.State.Action.PausePlayback
        ))
    }

    @Test
    fun saveMediaProgress_initialState_saveParameters() = runTest {
        //Given
        val isEnded = Random.nextBoolean()
        val mediaItemId = "mediaItemId"
        val position = Random.nextLong()

        val viewModel = viewModelBuilder.build()

        //When
        viewModel.saveMediaProgress(isEnded, mediaItemId, position)

        advanceUntilIdle()

        //Then
        with(fakeMediaProgressRepository.stateFlow.value) {
            assert(
                actions == listOf(
                    FakeMediaProgressRepository.State.Action.SetMediaProgress(
                        MediaProgressDO(isEnded, mediaItemId, position)
                    )
                )
            )
        }
    }

    @Test
    fun showPlayerViewControls_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.showPlayerViewControls()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState == startState)
    }

    @Test
    fun showPlayerViewControls_playingByPlayerView_addAction() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")
        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO, mediaProgressDO)
        }.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.showPlayerViewControls()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.actions.run { subList(startState.actions.size, size) } == listOf(
            NavigationActivityViewModel.State.Action.ShowPlayerViewControls
        ))
    }

    @Test
    fun unlockScreenOrientation_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.unlockScreenOrientation(0)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState == startState)
    }

    @Test
    fun unlockScreenOrientation_requestedScreenOrientationLandscape_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.RequestedScreenOrientationLandscape
        }.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.unlockScreenOrientation(90)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState == startState)
    }

    @Test
    fun unlockScreenOrientationWithLandscapeDeviceOrientation_requestedScreenOrientationPortrait_doNothing() =
        runTest {
            //Given
            val viewModel = viewModelBuilder.apply {
                testCase = ViewModelBuilder.TestCase.RequestedScreenOrientationPortrait
            }.build()
            val startState = viewModel.stateFlow.value

            //When
            viewModel.unlockScreenOrientation(90)

            advanceUntilIdle()

            //Then
            val endState = viewModel.stateFlow.value

            assert(endState == startState)
        }

    @Test
    fun unlockScreenOrientationWithPortraitDeviceOrientation_requestedScreenOrientationPortrait_setToUnspecified() =
        runTest {
            //Given
            val viewModel = viewModelBuilder.apply {
                testCase = ViewModelBuilder.TestCase.RequestedScreenOrientationPortrait
            }.build()

            //When
            viewModel.unlockScreenOrientation(0)

            advanceUntilIdle()

            //Then
            val endState = viewModel.stateFlow.value

            with(endState) {
                assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.UNSPECIFIED)
            }
        }
}
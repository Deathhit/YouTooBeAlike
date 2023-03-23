package com.deathhit.feature.navigation

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.model.MediaItemDO
import com.deathhit.domain.MediaProgressRepository
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.domain.enum_type.MediaItemLabel
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
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
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class NavigationActivityViewModelTest {
    private class ViewModelBuilder(
        private val mediaItemRepository: MediaItemRepository,
        private val mediaProgressRepository: MediaProgressRepository
    ) {
        sealed interface TestCase {
            object InitialState : TestCase
            object Fullscreen : TestCase
            object PlayerConnected : TestCase
            data class PlayingByPlayerView(val mediaItemId: String) : TestCase
            object RequestedScreenOrientationLandscape : TestCase
            object RequestedScreenOrientationPortrait : TestCase
            object ViewInLandscape : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = NavigationActivityViewModel(
            mediaItemRepository,
            mediaProgressRepository,
            SavedStateHandle.createHandle(null, Bundle.EMPTY)
        ).apply {
            runTest {
                when (val testCase = testCase) {
                    TestCase.Fullscreen -> {
                        setIsPlayerViewExpanded(true)
                        setIsViewInLandscape(true)
                    }
                    TestCase.PlayerConnected -> setIsPlayerConnected(true)
                    is TestCase.PlayingByPlayerView -> {
                        setIsPlayerConnected(true)
                        openItem(testCase.mediaItemId)
                    }
                    TestCase.RequestedScreenOrientationLandscape -> toggleScreenOrientation()
                    TestCase.RequestedScreenOrientationPortrait -> {
                        setIsViewInLandscape(true)
                        toggleScreenOrientation()
                        setIsViewInLandscape(false)
                    }
                    TestCase.ViewInLandscape -> setIsViewInLandscape(true)
                    else -> {}
                }

                advanceUntilIdle()
            }
        }
    }

    private class ViewModelStateAsserter(private val viewModel: NavigationActivityViewModel) {
        private val currentState get() = viewModel.stateFlow.value
        private val startState = viewModel.stateFlow.value

        fun assertInitialState() = with(currentState) {
            assert(actions.isEmpty())
            assert(attachedTabs.isEmpty())
            assert(!isFirstFrameRendered)
            assert(isForTabToPlay)
            assert(!isPlayerConnected)
            assert(!isPlayerViewExpanded)
            assert(!isViewInForeground)
            assert(!isViewInLandscape)
            assert(playItem == null)
            assert(playItemId == null)
            assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.UNSPECIFIED)
            assert(tab == NavigationActivityViewModel.State.Tab.HOME)
        }

        fun assertActionsIsEmpty() {
            assert(currentState.actions.isEmpty())
        }

        fun assertAttachedTabs(vararg tabs: NavigationActivityViewModel.State.Tab) {
            tabs.forEach {
                assert(currentState.attachedTabs.contains(it))
            }
        }

        fun assertIsFirstFrameRendered(isFirstFrameRendered: Boolean) {
            assert(currentState.isFirstFrameRendered == isFirstFrameRendered)
        }

        fun assertIsPlayerConnected(isPlayerConnected: Boolean) {
            assert(currentState.isPlayerConnected == isPlayerConnected)
        }

        fun assertIsPlayerViewExpanded(isPlayerViewExpanded: Boolean) {
            assert(currentState.isPlayerViewExpanded == isPlayerViewExpanded)
        }

        fun assertIsPlayingByPlayerView(isPlayingByPlayerView: Boolean) {
            assert(currentState.isPlayingByPlayerView == isPlayingByPlayerView)
        }

        fun assertIsViewInForeground(isViewInForeground: Boolean) {
            assert(currentState.isViewInForeground == isViewInForeground)
        }

        fun assertIsViewInLandscape(isViewInLandscape: Boolean) {
            assert(currentState.isViewInLandscape == isViewInLandscape)
        }

        fun assertLastActionCollapsePlayerView() {
            assert(currentState.actions.last() is NavigationActivityViewModel.State.Action.CollapsePlayerView)
        }

        fun assertLastActionPausePlayback() {
            assert(currentState.actions.last() is NavigationActivityViewModel.State.Action.PausePlayback)
        }

        fun assertLastActionShowPlayerViewControls() {
            assert(currentState.actions.last() is NavigationActivityViewModel.State.Action.ShowPlayerViewControls)
        }

        fun assertPlayItemPrepared(mediaItemVO: MediaItemVO, mediaProgressDO: MediaProgressDO) =
            with(currentState) {
                val lastAction = actions.last()
                assert(
                    lastAction is NavigationActivityViewModel.State.Action.PreparePlayback
                            && lastAction.isEnded == mediaProgressDO.isEnded
                            && lastAction.mediaItemId == mediaItemVO.id
                            && lastAction.position == mediaProgressDO.position
                            && lastAction.sourceUrl == mediaItemVO.sourceUrl
                )

                assert(playItem == mediaItemVO)
                assert(playItemId == mediaItemVO.id)
            }

        fun assertRequestedScreenOrientation(screenOrientation: NavigationActivityViewModel.State.ScreenOrientation) {
            assert(currentState.requestedScreenOrientation == screenOrientation)
        }

        fun assertStateUnchanged() {
            assert(currentState == startState)
        }

        fun assertTab(tab: NavigationActivityViewModel.State.Tab) {
            assert(currentState.tab == tab)
        }
    }

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val mediaItemRepository = object : MediaItemRepository {
        var mediaItemDO: MediaItemDO? = null

        override suspend fun clearByLabel(mediaItemLabel: MediaItemLabel) {

        }

        override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> =
            flowOf(mediaItemDO)

        override fun getMediaItemPagingDataFlow(
            exclusiveId: String?,
            mediaItemLabel: MediaItemLabel,
            subtitle: String?
        ): Flow<PagingData<MediaItemDO>> = flowOf(PagingData.empty())
    }

    private val mediaProgressRepository = object : MediaProgressRepository {
        var mediaProgressDO: MediaProgressDO? = null

        override suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressDO? =
            mediaProgressDO

        override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) {
            this.mediaProgressDO = mediaProgressDO
        }
    }

    private lateinit var viewModelBuilder: ViewModelBuilder

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModelBuilder = ViewModelBuilder(
            mediaItemRepository,
            mediaProgressRepository
        )
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
    fun addAttachedTab_initialState_addValue() = runTest {
        //Given
        val tab = NavigationActivityViewModel.State.Tab.values().run { get(Random.nextInt(size)) }

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.addAttachedTab(tab)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertAttachedTabs(tab)
    }

    @Test
    fun clearPlayerViewPlayback_playingByPlayerView_notPlayingByPlayerView() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO.mediaItemId)
        }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.clearPlayerViewPlayback()

        advanceUntilIdle()

        //Then
        with(viewModelStateAsserter) {
            assertIsFirstFrameRendered(false)
            assertIsPlayingByPlayerView(false)
        }
    }

    @Test
    fun collapsePlayerView_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.collapsePlayerView()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionCollapsePlayerView()
    }

    @Test
    fun collapsePlayerView_fullscreen_setOrientationToPortrait() = runTest {
        //Given
        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.Fullscreen }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.collapsePlayerView()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertRequestedScreenOrientation(NavigationActivityViewModel.State.ScreenOrientation.PORTRAIT)
    }

    @Test
    fun notifyFirstFrameRendered_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.notifyFirstFrameRendered("mediaItemId")

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun notifyFirstFrameRendered_playingByPlayerView_setValueToTrue() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO.mediaItemId)
        }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.notifyFirstFrameRendered(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsFirstFrameRendered(true)
    }

    @Test
    fun onAction_initialState_clearGivenAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        viewModel.collapsePlayerView()

        advanceUntilIdle()

        val action = viewModel.stateFlow.value.actions.last()

        //When
        viewModel.onAction(action)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertActionsIsEmpty()
    }

    @Test
    fun openItem_initialState_doNothing() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.openItem(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun openItem_playerConnected_preparePlayItem() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        mediaProgressRepository.mediaProgressDO = mediaProgressDO

        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.PlayerConnected }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.openItem(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertPlayItemPrepared(mediaItemDO.toMediaItemVO(), mediaProgressDO)
    }

    @Test
    fun pausePlayerViewPlayback_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.pausePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun pausePlayerViewPlayback_playingByPlayerView_addAction() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO.mediaItemId)
        }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.pausePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionPausePlayback()
    }

    @Test
    fun resumePlayerViewPlayback_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.resumePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun resumePlayerViewPlayback_playingByPlayerView_preparePlayItem() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        mediaProgressRepository.mediaProgressDO = mediaProgressDO

        val viewModel =
            viewModelBuilder.apply {
                testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO.mediaItemId)
            }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        viewModel.stateFlow.value.actions.forEach {
            viewModel.onAction(it)
        }

        advanceUntilIdle()

        //When
        viewModel.resumePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertPlayItemPrepared(mediaItemDO.toMediaItemVO(), mediaProgressDO)
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
        val savedMediaProgress = mediaProgressRepository.mediaProgressDO

        assert(
            savedMediaProgress != null
                    && savedMediaProgress.isEnded == isEnded
                    && savedMediaProgress.mediaItemId == mediaItemId
                    && savedMediaProgress.position == position
        )
    }

    @Test
    fun setIsPlayerConnected_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsPlayerConnected(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsPlayerConnected(true)
    }

    @Test
    fun setIsPlayerViewExpanded_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsPlayerViewExpanded(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsPlayerViewExpanded(true)
    }

    @Test
    fun setIsViewInForeground_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsViewInForeground(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsViewInForeground(true)
    }

    @Test
    fun setIsViewInLandscape_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsViewInLandscape(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsViewInLandscape(true)
    }

    @Test
    fun setTab_initialState_setValue() = runTest {
        //Given
        val tab = NavigationActivityViewModel.State.Tab.DASHBOARD

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setTab(tab)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertTab(tab)
    }

    @Test
    fun showPlayerViewControls_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.showPlayerViewControls()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun showPlayerViewControls_playingByPlayerView_addAction() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayingByPlayerView(mediaItemDO.mediaItemId)
        }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.showPlayerViewControls()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionShowPlayerViewControls()
    }

    @Test
    fun toggleScreenOrientation_initialState_setToLandscape() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.toggleScreenOrientation()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertRequestedScreenOrientation(NavigationActivityViewModel.State.ScreenOrientation.LANDSCAPE)
    }

    @Test
    fun toggleScreenOrientation_viewInLandscape_setToPortrait() = runTest {
        //Given
        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.ViewInLandscape }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.toggleScreenOrientation()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertRequestedScreenOrientation(NavigationActivityViewModel.State.ScreenOrientation.PORTRAIT)
    }

    @Test
    fun unlockScreenOrientation_initialState_doNothing() = runTest {
        //Given
        val deviceOrientation = Random.nextInt()

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.unlockScreenOrientation(deviceOrientation)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun unlockScreenOrientation_requestedScreenOrientationLandscape_doNothing() = runTest {
        //Given
        val deviceOrientation = Random.nextInt()

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.RequestedScreenOrientationLandscape
        }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.unlockScreenOrientation(deviceOrientation)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun unlockScreenOrientationWithLandscapeDeviceOrientation_requestedScreenOrientationPortrait_doNothing() =
        runTest {
            //Given
            val deviceOrientation = 90

            val viewModel = viewModelBuilder.apply {
                testCase = ViewModelBuilder.TestCase.RequestedScreenOrientationPortrait
            }.build()
            val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

            //When
            viewModel.unlockScreenOrientation(deviceOrientation)

            advanceUntilIdle()

            //Then
            viewModelStateAsserter.assertStateUnchanged()
        }

    @Test
    fun unlockScreenOrientationWithPortraitDeviceOrientation_requestedScreenOrientationPortrait_setToUnspecified() =
        runTest {
            //Given
            val deviceOrientation = 0

            val viewModel = viewModelBuilder.apply {
                testCase = ViewModelBuilder.TestCase.RequestedScreenOrientationPortrait
            }.build()
            val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

            //When
            viewModel.unlockScreenOrientation(deviceOrientation)

            advanceUntilIdle()

            //Then
            viewModelStateAsserter.assertRequestedScreenOrientation(NavigationActivityViewModel.State.ScreenOrientation.UNSPECIFIED)
        }
}
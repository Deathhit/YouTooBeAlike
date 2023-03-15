package com.deathhit.feature.navigation

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.data.media_progress.MediaProgressRepository
import com.deathhit.data.media_progress.model.MediaProgressDO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
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
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class NavigationActivityViewModelTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val mediaItemRepository = object : MediaItemRepository {
        var mediaItemDO: MediaItemDO? = null

        override suspend fun clearByLabel(mediaItemLabel: com.deathhit.data.media_item.model.MediaItemLabel) {

        }

        override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> =
            flowOf(mediaItemDO)

        override fun getMediaItemPagingDataFlow(
            exclusiveId: String?,
            mediaItemLabel: com.deathhit.data.media_item.model.MediaItemLabel,
            subtitle: String?
        ): Flow<PagingData<MediaItemDO>> = flowOf(PagingData.empty())
    }

    private val mediaProgressRepository = object : MediaProgressRepository {
        var savedMediaProgressDO: MediaProgressDO? = null

        override suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressDO? =
            savedMediaProgressDO

        override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) {
            this.savedMediaProgressDO = mediaProgressDO
        }
    }

    private lateinit var viewModel: NavigationActivityViewModel

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModel = NavigationActivityViewModel(
            mediaItemRepository,
            mediaProgressRepository,
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
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When

        //Then
        collectedState?.run {
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

        collectJob.cancel()
    }

    @Test
    fun addAttachedTabShouldAddValueToAttachedTabs() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val tab0 = NavigationActivityViewModel.State.Tab.values().run { get(Random.nextInt(size)) }
        val tab1 = NavigationActivityViewModel.State.Tab.values().filter { it != tab0 }
            .run { get(Random.nextInt(size)) }

        //When
        with(viewModel) {
            addAttachedTab(tab0)
            addAttachedTab(tab0)    //Test adding duplicate.
            addAttachedTab(tab1)
        }

        advanceUntilIdle()

        //Then
        collectedState?.run {
            assert(attachedTabs.size == 2)
            assert(attachedTabs.contains(tab0))
            assert(attachedTabs.contains(tab1))
        }

        collectJob.cancel()
    }

    @Test
    fun clearPlayerViewPlaybackShouldStopPlaybackAndClearRelatedStatus() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        viewModel.openItem(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //When
        viewModel.clearPlayerViewPlayback()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(actions.last() is NavigationActivityViewModel.State.Action.StopPlayback)
            assert(
                actions.dropLast(1)
                    .last() is NavigationActivityViewModel.State.Action.HidePlayerView
            )
            assert(!isFirstFrameRendered)
            assert(isForTabToPlay)
            assert(playItem == null)
            assert(playItemId == null)
        }

        collectJob.cancel()
    }

    @Test
    fun collapsePlayerViewShouldAddCorrespondingAction() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When
        viewModel.collapsePlayerView()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(actions.last() is NavigationActivityViewModel.State.Action.CollapsePlayerView)
        }

        collectJob.cancel()
    }

    @Test
    fun collapsePlayerViewShouldSetScreenOrientationToPortraitIfIsFullscreen() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        viewModel.setIsViewInLandscape(true)
        viewModel.setIsPlayerViewExpanded(true)

        advanceUntilIdle()

        //When
        viewModel.collapsePlayerView()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(requestedScreenOrientation == NavigationActivityViewModel.State.ScreenOrientation.PORTRAIT)
        }

        collectJob.cancel()
    }

    @Test
    fun notifyFirstFrameRenderedShouldSetValueTrue() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When
        viewModel.notifyFirstFrameRendered()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(isFirstFrameRendered)
        }

        collectJob.cancel()
    }

    @Test
    fun onActionShouldReduceGivenAction() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        viewModel.collapsePlayerView()

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
    fun openItemShouldPrepareAndPlayGivenItem() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        mediaProgressRepository.savedMediaProgressDO = mediaProgressDO

        //When
        with(viewModel) {
            setIsPlayerConnected(true)
            openItem(mediaItemDO.mediaItemId)
        }

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            var action = actions.run { get(lastIndex) }

            assert(
                action is NavigationActivityViewModel.State.Action.PreparePlayback
                        && action.isEnded == mediaProgressDO.isEnded
                        && action.position == mediaProgressDO.position
                        && action.sourceUrl == mediaItemDO.sourceUrl
            )

            action = actions.run { get(lastIndex - 1) }

            assert(action is NavigationActivityViewModel.State.Action.StopPlayback)

            action = actions.run { get(lastIndex - 2) }

            assert(action is NavigationActivityViewModel.State.Action.PlayPlayback)

            action = actions.run { get(lastIndex - 3) }

            assert(action is NavigationActivityViewModel.State.Action.ExpandPlayerView)
            assert(!isForTabToPlay)
            assert(playItem == mediaItemDO.toMediaItemVO())
            assert(playItemId == mediaItemDO.mediaItemId)
        }

        collectJob.cancel()
    }

    @Test
    fun pausePlayerViewPlaybackShouldAddCorrespondingAction() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        with(viewModel) {
            setIsPlayerConnected(true)
            openItem(mediaItemDO.mediaItemId)
        }

        advanceUntilIdle()

        //When
        viewModel.pausePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(actions.last() is NavigationActivityViewModel.State.Action.PausePlayback)
        }

        collectJob.cancel()
    }

    @Test
    fun resumePlayerViewPlaybackShouldAddCorrespondingAction() = runTest {
        //Given
        var collectedState: NavigationActivityViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        with(viewModel) {
            setIsPlayerConnected(true)
            openItem(mediaItemDO.mediaItemId)
        }

        advanceUntilIdle()

        //When
        viewModel.resumePlayerViewPlayback()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            val preparePlaybackActions =
                actions.filterIsInstance<NavigationActivityViewModel.State.Action.PreparePlayback>()

            assert(preparePlaybackActions.size == 2)
            assert(preparePlaybackActions.last() == preparePlaybackActions.run { get(lastIndex - 1) })
        }

        collectJob.cancel()
    }

    @Test
    fun savePlayItemPositionShouldSaveGivenPositionForPlayItem() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")

        mediaItemRepository.mediaItemDO = mediaItemDO

        with(viewModel) {
            setIsPlayerConnected(true)
            openItem(mediaItemDO.mediaItemId)
        }

        advanceUntilIdle()

        //When
        val isEnded = Random.nextBoolean()
        val position = Random.nextLong()

        viewModel.saveMediaProgress(isEnded, position)

        advanceUntilIdle()

        //Then
        val savedMediaProgress = mediaProgressRepository.savedMediaProgressDO

        assert(
            savedMediaProgress != null
                    && savedMediaProgress.isEnded == isEnded
                    && savedMediaProgress.mediaItemId == mediaItemDO.mediaItemId
                    && savedMediaProgress.position == position
        )
    }
    //todo finish implementation
}
package com.deathhit.feature.media_item_list

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.data.media_progress.MediaProgressRepository
import com.deathhit.data.media_progress.model.MediaProgressDO
import com.deathhit.feature.media_item_list.model.MediaItemLabel
import com.deathhit.feature.media_item_list.model.MediaItemVO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
class MediaItemListViewModelTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val mediaItemLabel = MediaItemLabel.values().run { get(Random.nextInt(size)) }

    private val mediaItemRepository = object : MediaItemRepository {
        override suspend fun clearByLabel(mediaItemLabel: com.deathhit.data.media_item.model.MediaItemLabel) {

        }

        override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> = emptyFlow()

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

    private lateinit var viewModel: MediaItemListViewModel

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModel = MediaItemListViewModel(
            mediaItemRepository,
            mediaProgressRepository,
            SavedStateHandle.createHandle(null, MediaItemListViewModel.createArgs(mediaItemLabel))
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
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When

        //Then
        collectedState?.run {
            assert(actions.isEmpty())
            assert(firstCompletelyVisibleItemPosition == null)
            assert(!isFirstFrameRendered)
            assert(!isFirstPageLoaded)
            assert(!isPlayerSet)
            assert(!isRefreshingList)
            assert(!isViewActive)
            assert(!isViewHidden)
            assert(!isViewInLandscape)
            assert(mediaItemLabel == this@MediaItemListViewModelTest.mediaItemLabel)
            assert(playItem == null)
        }

        collectJob.cancel()
    }

    @Test
    fun notifyFirstFrameRenderedShouldSetValue() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
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
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        viewModel.refreshList()

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
        var collectedState: MediaItemListViewModel.State? = null
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

            assert(action is MediaItemListViewModel.State.Action.OpenItem && action.item == mediaItem)
        }

        collectJob.cancel()
    }

    @Test
    fun prepareItemShouldSetValueAndAddCorrespondingAction() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        viewModel.setIsPlayerSet(true)

        advanceUntilIdle()

        val mediaItem0 = MediaItemVO("0", "sourceUrl0", "subtitle", "thumbUrl", "title")
        val mediaItem1 = MediaItemVO("1", "sourceUrl1", "subtitle", "thumbUrl", "title")

        val mediaProgress1 = MediaProgressDO(
            Random.nextBoolean(),
            mediaItem1.id,
            Random.nextLong()
        ).also { mediaProgressRepository.savedMediaProgressDO = it }

        //When
        viewModel.prepareItem(mediaItem0)
        viewModel.prepareItem(mediaItem1)   //The call should cancel the previous one.

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(actions.filterIsInstance<MediaItemListViewModel.State.Action.PrepareAndPlayPlayback>().size == 1)

            val action = actions.last()

            assert(
                action is MediaItemListViewModel.State.Action.PrepareAndPlayPlayback
                        && action.isEnded == mediaProgress1.isEnded
                        && action.sourceUrl == mediaItem1.sourceUrl
                        && action.position == mediaProgress1.position
            )
            assert(playItem == mediaItem1)
        }

        collectJob.cancel()
    }

    @Test
    fun refreshListShouldAddTheCorrespondingAction() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When
        viewModel.refreshList()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            val action = actions.last()

            assert(action is MediaItemListViewModel.State.Action.RefreshList)
        }

        collectJob.cancel()
    }

    @Test
    fun retryLoadingListListShouldAddTheCorrespondingAction() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        //When
        viewModel.retryLoadingList()

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            val action = actions.last()

            assert(action is MediaItemListViewModel.State.Action.RetryLoadingList)
        }

        collectJob.cancel()
    }

    @Test
    fun savePlayItemPositionShouldSaveGivenPositionForPlayItem() = runTest {
        //Given
        val playItem = MediaItemVO("0", "sourceUrl0", "subtitle", "thumbUrl", "title")

        viewModel.prepareItem(playItem)

        advanceUntilIdle()

        //When
        val isEnded = Random.nextBoolean()
        val position = Random.nextLong()

        viewModel.savePlayItemPosition(isEnded, position)

        advanceUntilIdle()

        //Then
        val savedMediaProgress = mediaProgressRepository.savedMediaProgressDO

        assert(
            savedMediaProgress != null
                    && savedMediaProgress.isEnded == isEnded
                    && savedMediaProgress.mediaItemId == playItem.id
                    && savedMediaProgress.position == position
        )
    }

    @Test
    fun scrollToTopOnFirstPageLoadedShouldSetValueAndAddCorrespondingAction() =
        runTest {
            //Given
            var collectedState: MediaItemListViewModel.State? = null
            val collectJob = launch {
                viewModel.stateFlow.collect {
                    collectedState = it
                }
            }

            //When
            viewModel.scrollToTopOnFirstPageLoaded()
            viewModel.scrollToTopOnFirstPageLoaded()    //This should be ignored because of the first call.

            advanceUntilIdle()

            //Then
            collectedState!!.run {
                assert(actions.filterIsInstance<MediaItemListViewModel.State.Action.ScrollToTop>().size == 1)
                assert(actions.last() is MediaItemListViewModel.State.Action.ScrollToTop)
                assert(isFirstPageLoaded)
            }

            collectJob.cancel()
        }

    @Test
    fun setFirstCompletelyVisibleItemPositionShouldSetValue() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val position = Random.nextInt()

        //When
        viewModel.setFirstCompletelyVisibleItemPosition(position)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(firstCompletelyVisibleItemPosition == position)
        }

        collectJob.cancel()
    }

    @Test
    fun setIsPlayerSetShouldSetValue() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val isPlayerSet = Random.nextBoolean()

        //When
        viewModel.setIsPlayerSet(isPlayerSet)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(this.isPlayerSet == isPlayerSet)
        }

        collectJob.cancel()
    }

    @Test
    fun setIsViewActiveShouldSetValue() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val isViewActive = Random.nextBoolean()

        //When
        viewModel.setIsViewActive(isViewActive)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(this.isViewActive == isViewActive)
        }

        collectJob.cancel()
    }

    @Test
    fun setIsViewHiddenShouldSetValue() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val isViewHidden = Random.nextBoolean()

        //When
        viewModel.setIsViewHidden(isViewHidden)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(this.isViewHidden == isViewHidden)
        }

        collectJob.cancel()
    }

    @Test
    fun setIsViewInLandscapeShouldSetValue() = runTest {
        //Given
        var collectedState: MediaItemListViewModel.State? = null
        val collectJob = launch {
            viewModel.stateFlow.collect {
                collectedState = it
            }
        }

        val isViewInLandscape = Random.nextBoolean()

        //When
        viewModel.setIsViewInLandscape(isViewInLandscape)

        advanceUntilIdle()

        //Then
        collectedState!!.run {
            assert(this.isViewInLandscape == isViewInLandscape)
        }

        collectJob.cancel()
    }
}
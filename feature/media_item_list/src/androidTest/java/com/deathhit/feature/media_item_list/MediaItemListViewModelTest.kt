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
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MediaItemListViewModelTest {
    private class ViewModelBuilder(
        private val mediaItemLabel: MediaItemLabel,
        private val mediaItemRepository: MediaItemRepository,
        private val mediaProgressRepository: MediaProgressRepository
    ) {
        sealed interface TestCase {
            object InitialState : TestCase
            object FirstPageLoaded : TestCase
            object ReadyToPlay : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = MediaItemListViewModel(
            mediaItemRepository,
            mediaProgressRepository,
            SavedStateHandle.createHandle(null, MediaItemListViewModel.createArgs(mediaItemLabel))
        ).apply {
            when(testCase) {
                TestCase.FirstPageLoaded -> scrollToTopOnFirstPageLoaded()
                TestCase.ReadyToPlay -> {
                    setFirstCompletelyVisibleItemPosition(Random.nextInt())
                    setIsPlayerSet(true)
                    setIsViewActive(true)
                    setIsViewHidden(false)
                    setIsViewInLandscape(false)
                }
                else -> {}
            }

            runTest { advanceUntilIdle() }
        }
    }

    private class ViewModelStateAsserter(private val viewModel: MediaItemListViewModel) {
        private val currentState get() = viewModel.stateFlow.value
        private val startState = viewModel.stateFlow.value

        fun assertInitialState() = with(currentState) {
            assert(actions.isEmpty())
            assert(firstCompletelyVisibleItemPosition == null)
            assert(!isFirstFrameRendered)
            assert(!isFirstPageLoaded)
            assert(!isPlayerSet)
            assert(!isRefreshingList)
            assert(!isViewActive)
            assert(!isViewHidden)
            assert(!isViewInLandscape)
            assert(mediaItemLabel == startState.mediaItemLabel)
            assert(playItem == null)
        }

        fun assertActionsIsEmpty() {
            assert(currentState.actions.isEmpty())
        }

        fun assertFirstCompletelyVisiblePositionIsValue(value: Int?) {
            assert(currentState.firstCompletelyVisibleItemPosition == value)
        }

        fun assertIsFirstFrameRendered() {
            assert(currentState.isFirstFrameRendered)
        }

        fun assertIsFirstPageLoaded() {
            assert(currentState.isFirstPageLoaded)
        }

        fun assertIsPlayerSet() {
            assert(currentState.isPlayerSet)
        }

        fun assertIsReadyToPlay() {
            assert(currentState.isReadyToPlay)
        }

        fun assertIsRefreshingList() {
            assert(currentState.isRefreshingList)
        }

        fun assertIsViewActive() {
            assert(currentState.isViewActive)
        }

        fun assertIsViewHidden() {
            assert(currentState.isViewHidden)
        }

        fun assertIsViewInLandscape() {
            assert(currentState.isViewInLandscape)
        }

        fun assertLastActionIsOpenItem(item: MediaItemVO) =
            currentState.actions.last().let {
                assert(it is MediaItemListViewModel.State.Action.OpenItem && it.item == item)
            }

        fun assertLastActionIsPrepareAndPlayPlaybackWithGivenParameters(
            isEnded: Boolean,
            mediaItemId: String,
            position: Long,
            sourceUrl: String
        ) = currentState.actions.last().let {
            assert(
                it is MediaItemListViewModel.State.Action.PrepareAndPlayPlayback
                        && it.isEnded == isEnded
                        && it.mediaItemId == mediaItemId
                        && it.position == position
                        && it.sourceUrl == sourceUrl
            )
        }

        fun assertLastActionIsRefreshList() {
            assert(currentState.actions.last() is MediaItemListViewModel.State.Action.RefreshList)
        }

        fun assertLastActionIsRetryLoadingList() {
            assert(currentState.actions.last() is MediaItemListViewModel.State.Action.RetryLoadingList)
        }

        fun assertLastActionIsScrollToTop() {
            assert(currentState.actions.last() is MediaItemListViewModel.State.Action.ScrollToTop)
        }

        fun assertPlayItemIsItem(item: MediaItemVO) {
            assert(currentState.playItem == item)
        }

        fun assertStateUnchanged() {
            assert(currentState == startState)
        }
    }

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
            mediaItemLabel,
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
    fun notifyFirstFrameRendered_initialState_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.notifyFirstFrameRendered()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun notifyFirstFrameRendered_readyToPlay_setValueToTrue() = runTest {
        //Given
        val viewModel = viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.ReadyToPlay }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.notifyFirstFrameRendered()

        advanceUntilIdle()

        //Then
        with(viewModelStateAsserter) {
            assertIsFirstFrameRendered()
            assertIsReadyToPlay()
        }
    }

    @Test
    fun onAction_initialState_clearGivenAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        viewModel.refreshList()

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
    fun prepareItemIfNotePrepared_initialState_doNothing() = runTest {
        //Given
        val mediaItem = MediaItemVO("id", "sourceUrl", "subtitle", "thumbUrl", "title")

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.prepareItemIfNotPrepared(mediaItem)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun prepareItemIfNotePrepared_readyToPlay_addAction() = runTest {
        //Given
        val mediaItem = MediaItemVO("id", "sourceUrl", "subtitle", "thumbUrl", "title")

        val mediaProgress = MediaProgressDO(
            Random.nextBoolean(),
            mediaItem.id,
            Random.nextLong()
        ).also { mediaProgressRepository.mediaProgressDO = it }

        val viewModel = viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.ReadyToPlay }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.prepareItemIfNotPrepared(mediaItem)

        advanceUntilIdle()

        //Then
        with(viewModelStateAsserter) {
            assertIsReadyToPlay()
            assertLastActionIsPrepareAndPlayPlaybackWithGivenParameters(
                mediaProgress.isEnded,
                mediaItem.id,
                mediaProgress.position,
                mediaItem.sourceUrl
            )
            assertPlayItemIsItem(mediaItem)
        }
    }

    @Test
    fun refreshList_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.refreshList()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionIsRefreshList()
    }

    @Test
    fun retryLoadingList_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.retryLoadingList()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionIsRetryLoadingList()
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
    fun scrollToTopOnFirstPageLoaded_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.scrollToTopOnFirstPageLoaded()

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertLastActionIsScrollToTop()
    }

    @Test
    fun scrollToTopOnFirstPageLoaded_firstPageLoaded_doNothing() = runTest {
        //Given
        val viewModel = viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.FirstPageLoaded }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.scrollToTopOnFirstPageLoaded()

        advanceUntilIdle()

        //Then
        with(viewModelStateAsserter) {
            assertIsFirstPageLoaded()
            assertStateUnchanged()
        }
    }

    @Test
    fun setFirstCompletelyVisibleItemPosition_initialState_setValue() = runTest {
        //Given
        val position = Random.nextInt()

        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setFirstCompletelyVisibleItemPosition(position)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertFirstCompletelyVisiblePositionIsValue(position)
    }

    @Test
    fun setIsPlayerSet_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsPlayerSet(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsPlayerSet()
    }

    @Test
    fun setIsRefreshingList_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsRefreshingList(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsRefreshingList()
    }

    @Test
    fun setIsViewActive_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsViewActive(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsViewActive()
    }

    @Test
    fun setIsViewHidden_initialState_setValue() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.setIsViewHidden(true)

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertIsViewHidden()
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
        viewModelStateAsserter.assertIsViewInLandscape()
    }
}
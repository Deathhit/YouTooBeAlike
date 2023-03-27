package com.deathhit.feature.media_item_list

import androidx.lifecycle.SavedStateHandle
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.MediaProgressRepository
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.domain.test.FakeMediaItemRepository
import com.deathhit.domain.test.FakeMediaProgressRepository
import com.deathhit.feature.media_item_list.enum_type.MediaItemLabel
import com.deathhit.feature.media_item_list.model.MediaItemVO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
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
            data class PlayItemPrepared(val mediaItem: MediaItemVO) : TestCase
            object ReadyToPlay : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = MediaItemListViewModel(
            mediaItemRepository,
            mediaProgressRepository,
            SavedStateHandle.createHandle(null, MediaItemListViewModel.createArgs(mediaItemLabel))
        ).apply {
            runTest {
                when (val testCase = testCase) {
                    TestCase.FirstPageLoaded -> scrollToTopOnFirstPageLoaded()
                    is TestCase.PlayItemPrepared -> {
                        setFirstCompletelyVisibleItemPosition(Random.nextInt())
                        setIsPlayerSet(true)
                        setIsViewActive(true)
                        setIsViewHidden(false)
                        setIsViewInLandscape(false)

                        advanceUntilIdle()

                        prepareItemIfNotPrepared(testCase.mediaItem)
                    }
                    TestCase.ReadyToPlay -> {
                        setFirstCompletelyVisibleItemPosition(Random.nextInt())
                        setIsPlayerSet(true)
                        setIsViewActive(true)
                        setIsViewHidden(false)
                        setIsViewInLandscape(false)
                    }
                    else -> {}
                }

                advanceUntilIdle()
            }
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

    @Inject
    lateinit var fakeMediaItemRepository: FakeMediaItemRepository

    @Inject
    lateinit var fakeMediaProgressRepository: FakeMediaProgressRepository

    private val mediaItemLabel = MediaItemLabel.values().run { get(Random.nextInt(size)) }

    private lateinit var viewModelBuilder: ViewModelBuilder

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())

        hiltRule.inject()

        viewModelBuilder = ViewModelBuilder(
            mediaItemLabel,
            fakeMediaItemRepository,
            fakeMediaProgressRepository
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
        viewModel.notifyFirstFrameRendered("mediaItemId")

        advanceUntilIdle()

        //Then
        viewModelStateAsserter.assertStateUnchanged()
    }

    @Test
    fun notifyFirstFrameRendered_playItemPrepared_setValueToTrue() = runTest {
        //Given
        val mediaItem = MediaItemVO("id", "sourceUrl", "subtitle", "thumbUrl", "title")

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayItemPrepared(mediaItem)
        }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.notifyFirstFrameRendered(mediaItem.id)

        advanceUntilIdle()

        //Then
        with(viewModelStateAsserter) {
            assertIsFirstFrameRendered()
            assertIsReadyToPlay()
            assertPlayItemIsItem(mediaItem)
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
        )

        fakeMediaProgressRepository.funcGetMediaProgressByMediaItemId =
            { mediaItemId -> if (mediaItemId == mediaProgress.mediaItemId) mediaProgress else null }

        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.ReadyToPlay }.build()
        val viewModelStateAsserter = ViewModelStateAsserter(viewModel)

        //When
        viewModel.prepareItemIfNotPrepared(mediaItem)

        advanceUntilIdle()

        //Then
        with(fakeMediaProgressRepository.stateFlow.value) {
            assert(
                actions == listOf(
                    FakeMediaProgressRepository.State.Action.GetMediaProgressByMediaItemId(
                        mediaItemId = mediaProgress.mediaItemId
                    )
                )
            )
        }

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
        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.FirstPageLoaded }.build()
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
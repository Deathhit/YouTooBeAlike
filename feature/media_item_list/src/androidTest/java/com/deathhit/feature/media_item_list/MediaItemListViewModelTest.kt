package com.deathhit.feature.media_item_list

import androidx.lifecycle.SavedStateHandle
import com.deathhit.domain.model.MediaItemDO
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.domain.test.FakeMediaItemRepository
import com.deathhit.domain.test.FakeMediaProgressRepository
import com.deathhit.feature.media_item_list.enum_type.MediaItemLabel
import com.deathhit.feature.media_item_list.model.MediaItemVO
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
class MediaItemListViewModelTest {
    private class ViewModelBuilder(
        private val fakeMediaItemRepository: FakeMediaItemRepository,
        private val fakeMediaProgressRepository: FakeMediaProgressRepository,
        private val mediaItemLabel: MediaItemLabel
    ) {
        sealed interface TestCase {
            object FirstPageLoaded : TestCase
            object InitialState : TestCase
            data class PlayItemPrepared(
                val mediaItemDO: MediaItemDO,
                val mediaProgressDO: MediaProgressDO
            ) : TestCase
        }

        var testCase: TestCase = TestCase.InitialState

        fun build() = when (val testCase = testCase) {
            TestCase.FirstPageLoaded -> createViewModel().apply {
                runTest {
                    //Given

                    //When
                    scrollToTopOnFirstPageLoaded()

                    advanceUntilIdle()

                    //Then
                    with(stateFlow.value) {
                        assert(isFirstPageLoaded)
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
                        assert(actions == listOf(MediaItemListViewModel.State.Action.StopPlayback))
                        assert(firstCompletelyVisibleItemPosition == null)
                        assert(!isFirstFrameRendered)
                        assert(!isFirstPageLoaded)
                        assert(!isPlayerSet)
                        assert(!isRefreshingList)
                        assert(!isViewActive)
                        assert(!isViewHidden)
                        assert(!isViewInLandscape)
                        assert(mediaItemLabel == this@ViewModelBuilder.mediaItemLabel)
                        assert(playItemId == null)
                    }
                }
            }
            is TestCase.PlayItemPrepared -> {
                //Given
                val mediaItemDO = testCase.mediaItemDO
                val mediaProgressDO = testCase.mediaProgressDO

                with(fakeMediaItemRepository) {
                    funcGetMediaItemFlowById =
                        { mediaItemId -> flowOf(if (mediaItemId == mediaItemDO.mediaItemId) mediaItemDO else null) }
                }

                with(fakeMediaProgressRepository) {
                    funcGetMediaProgressByMediaItemId =
                        { mediaItemId -> if (mediaItemId == mediaProgressDO.mediaItemId) mediaProgressDO else null }
                }

                createViewModel().apply {
                    runTest {
                        //Given
                        val firstCompletelyVisibleItemPosition = Random.nextInt()
                        val isPlayerSet = true
                        val isViewActive = true
                        val isViewHidden = false
                        val isViewInLandscape = false
                        val playItemId = mediaItemDO.mediaItemId

                        //When
                        setFirstCompletelyVisibleItemPosition(firstCompletelyVisibleItemPosition)
                        setIsPlayerSet(isPlayerSet)
                        setIsViewActive(isViewActive)
                        setIsViewHidden(isViewHidden)
                        setIsViewInLandscape(isViewInLandscape)

                        advanceUntilIdle()

                        setPlayItemId(playItemId)

                        advanceUntilIdle()

                        //Then
                        with(fakeMediaItemRepository.stateFlow.value) {
                            assert(
                                actions == listOf(
                                    FakeMediaItemRepository.State.Action.GetMediaItemFlowById(
                                        mediaItemId = mediaItemDO.mediaItemId
                                    )
                                )
                            )
                        }

                        with(fakeMediaProgressRepository.stateFlow.value) {
                            assert(
                                actions == listOf(
                                    FakeMediaProgressRepository.State.Action.GetMediaProgressByMediaItemId(
                                        mediaItemId = mediaProgressDO.mediaItemId
                                    )
                                )
                            )
                        }

                        with(stateFlow.value) {
                            assert(
                                actions == listOf(
                                    MediaItemListViewModel.State.Action.StopPlayback,
                                    MediaItemListViewModel.State.Action.PrepareAndPlayPlayback(
                                        mediaProgressDO.isEnded,
                                        mediaItemDO.mediaItemId,
                                        mediaProgressDO.position,
                                        mediaItemDO.sourceUrl
                                    )
                                )
                            )
                            assert(this.firstCompletelyVisibleItemPosition == firstCompletelyVisibleItemPosition)
                            assert(this.isPlayerSet == isPlayerSet)
                            assert(this.isViewActive == isViewActive)
                            assert(this.isViewHidden == isViewHidden)
                            assert((this.isViewInLandscape == isViewInLandscape))
                            assert(this.playItemId == playItemId)
                        }
                    }
                }
            }
        }

        private fun createViewModel() = MediaItemListViewModel(
            fakeMediaItemRepository,
            fakeMediaProgressRepository,
            SavedStateHandle.createHandle(null, MediaItemListViewModel.createArgs(mediaItemLabel))
        )
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

        viewModelBuilder =
            ViewModelBuilder(fakeMediaItemRepository, fakeMediaProgressRepository, mediaItemLabel)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
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

        assert(!endState.isFirstFrameRendered)
        assert(endState.actions - startState.actions.toSet() == emptyList<MediaItemListViewModel.State.Action>())
    }

    @Test
    fun notifyFirstFrameRendered_playItemPrepared_setValueToTrue() = runTest {
        //Given
        val mediaItemDO =
            MediaItemDO("description", "mediaItemId", "sourceUrl", "subtitle", "thumbUrl", "title")
        val mediaProgressDO =
            MediaProgressDO(Random.nextBoolean(), mediaItemDO.mediaItemId, Random.nextLong())

        val viewModel = viewModelBuilder.apply {
            testCase = ViewModelBuilder.TestCase.PlayItemPrepared(mediaItemDO, mediaProgressDO)
        }.build()

        //When
        viewModel.notifyFirstFrameRendered(mediaItemDO.mediaItemId)

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.isFirstFrameRendered)
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

        assert(endState.actions - startState.actions.toSet() == listOf(MediaItemListViewModel.State.Action.OpenItem(mediaItemVO)))
    }

    @Test
    fun refreshList_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.refreshList()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.actions - startState.actions.toSet() == listOf(MediaItemListViewModel.State.Action.RefreshList))
    }

    @Test
    fun retryLoadingList_initialState_addAction() = runTest {
        //Given
        val viewModel = viewModelBuilder.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.retryLoadingList()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.actions - startState.actions.toSet() == listOf(MediaItemListViewModel.State.Action.RetryLoadingList))
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
        val startState = viewModel.stateFlow.value

        //When
        viewModel.scrollToTopOnFirstPageLoaded()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.isFirstPageLoaded)
        assert(endState.actions - startState.actions.toSet() == listOf(MediaItemListViewModel.State.Action.ScrollToTop))
    }

    @Test
    fun scrollToTopOnFirstPageLoaded_firstPageLoaded_doNothing() = runTest {
        //Given
        val viewModel =
            viewModelBuilder.apply { testCase = ViewModelBuilder.TestCase.FirstPageLoaded }.build()
        val startState = viewModel.stateFlow.value

        //When
        viewModel.scrollToTopOnFirstPageLoaded()

        advanceUntilIdle()

        //Then
        val endState = viewModel.stateFlow.value

        assert(endState.isFirstPageLoaded)
        assert(endState.actions - startState.actions.toSet() == emptyList<MediaItemListViewModel.State.Action>())
    }
}
package com.deathhit.feature.media_item_list

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.MediaProgressRepository
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.feature.media_item_list.enum_type.MediaItemLabel
import com.deathhit.feature.media_item_list.enum_type.toMediaItemLabelDO
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaItemListViewModel @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaProgressRepository: MediaProgressRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "MediaItemListViewModel"
        private const val KEY_MEDIA_ITEM_LABEL = "$TAG.KEY_MEDIA_ITEM_LABEL"

        private const val MEDIA_SWITCHING_DELAY = 500L

        fun createArgs(mediaItemLabel: MediaItemLabel) =
            Bundle().apply {
                putSerializable(KEY_MEDIA_ITEM_LABEL, mediaItemLabel)
            }
    }

    data class State(
        val actions: List<Action>,
        val firstCompletelyVisibleItemPosition: Int?,
        val isFirstFrameRendered: Boolean,
        val isFirstPageLoaded: Boolean,
        val isPlayerSet: Boolean,
        val isRefreshingList: Boolean,
        val isViewActive: Boolean,
        val isViewHidden: Boolean,
        val isViewInLandscape: Boolean,
        val mediaItemLabel: MediaItemLabel,
        val playItemId: String?
    ) {
        sealed interface Action {
            data class OpenItem(val item: MediaItemVO) : Action
            data class PrepareAndPlayPlayback(
                val isEnded: Boolean,
                val mediaItemId: String,
                val position: Long?,
                val sourceUrl: String
            ) : Action

            object RefreshList : Action
            object RetryLoadingList : Action
            object ScrollToTop : Action
            object StopPlayback : Action
        }

        data class ListState(
            val isFirstFrameRendered: Boolean,
            val playPosition: Int?
        )

        val playPosition =
            if (isPlayerSet && isViewActive && !isViewHidden && !isViewInLandscape) firstCompletelyVisibleItemPosition else null
        val listState = ListState(isFirstFrameRendered, playPosition)
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                firstCompletelyVisibleItemPosition = null,
                isFirstFrameRendered = false,
                isFirstPageLoaded = false,
                isPlayerSet = false,
                isRefreshingList = false,
                isViewActive = false,
                isViewHidden = false,
                isViewInLandscape = false,
                mediaItemLabel = savedStateHandle[KEY_MEDIA_ITEM_LABEL]
                    ?: throw RuntimeException("mediaItemLabel can not be null!"),
                playItemId = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        stateFlow.map { it.mediaItemLabel }.distinctUntilChanged().flatMapLatest {
            _stateFlow.update { state ->
                state.copy(isFirstPageLoaded = false)
            }

            mediaItemRepository.getMediaItemPagingDataFlow(mediaItemLabel = mediaItemLabel.toMediaItemLabelDO())
                .map { pagingData -> pagingData.map { it.toMediaItemVO() } }
        }.cachedIn(viewModelScope)

    private val isFirstPageLoaded get() = stateFlow.value.isFirstPageLoaded
    private val mediaItemLabel get() = stateFlow.value.mediaItemLabel
    private val playItemId get() = stateFlow.value.playItemId

    private var prepareItemJob: Job? = null

    init {
        viewModelScope.launch {
            launch {
                stateFlow.map { it.playItemId }.distinctUntilChanged().collectLatest {
                    prepareItemJob?.cancel()

                    if (it == null)
                        return@collectLatest

                    prepareItemJob = viewModelScope.launch {
                        prepareItem(it)
                    }
                }
            }

            launch {
                stateFlow.map { it.playPosition }.distinctUntilChanged().collectLatest {
                    _stateFlow.update { state ->
                        state.copy(
                            actions = state.actions + State.Action.StopPlayback,
                            isFirstFrameRendered = false,
                            playItemId = null
                        )
                    }
                }
            }
        }
    }

    fun notifyFirstFrameRendered(mediaItemId: String) {
        if (mediaItemId != playItemId)
            return

        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = true)
        }
    }

    fun onAction(action: State.Action) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions - action)
        }
    }

    fun openItem(item: MediaItemVO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.OpenItem(item))
        }
    }

    fun refreshList() {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.RefreshList)
        }
    }

    fun retryLoadingList() {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.RetryLoadingList)
        }
    }

    fun saveMediaProgress(isEnded: Boolean, mediaItemId: String, mediaPosition: Long) {
        viewModelScope.launch(NonCancellable) {
            mediaProgressRepository.setMediaProgress(
                MediaProgressDO(
                    isEnded,
                    mediaItemId,
                    mediaPosition
                )
            )
        }
    }

    fun saveState() {
        savedStateHandle[KEY_MEDIA_ITEM_LABEL] = mediaItemLabel
    }

    fun scrollToTopOnFirstPageLoaded() {
        if (isFirstPageLoaded)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ScrollToTop, isFirstPageLoaded = true)
        }
    }

    fun setFirstCompletelyVisibleItemPosition(firstCompletelyVisibleItemPosition: Int?) {
        _stateFlow.update { state ->
            state.copy(firstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPosition)
        }
    }

    fun setIsPlayerSet(isPlayerSet: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlayerSet = isPlayerSet)
        }
    }

    fun setIsRefreshingList(isRefreshingList: Boolean) {
        _stateFlow.update { state ->
            state.copy(isRefreshingList = isRefreshingList)
        }
    }

    fun setIsViewActive(isViewActive: Boolean) {
        _stateFlow.update { state ->
            state.copy(isViewActive = isViewActive)
        }
    }

    fun setIsViewHidden(isViewHidden: Boolean) {
        _stateFlow.update { state ->
            state.copy(isViewHidden = isViewHidden)
        }
    }

    fun setIsViewInLandscape(isViewInLandscape: Boolean) {
        _stateFlow.update { state ->
            state.copy(isViewInLandscape = isViewInLandscape)
        }
    }

    fun setPlayItemId(playItemId: String?) {
        _stateFlow.update { state ->
            state.copy(playItemId = playItemId)
        }
    }

    private suspend fun prepareItem(playItemId: String) {
        delay(MEDIA_SWITCHING_DELAY)

        val playItemDO = mediaItemRepository.getMediaItemFlowById(playItemId).first()

        if (playItemDO != null)
            _stateFlow.update { state ->
                val progress =
                    mediaProgressRepository.getMediaProgressByMediaItemId(playItemId)

                state.copy(
                    actions = state.actions + State.Action.PrepareAndPlayPlayback(
                        progress?.isEnded ?: false,
                        playItemId,
                        progress?.position,
                        playItemDO.sourceUrl
                    )
                )
            }
    }
}
package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_progress.model.MediaProgressDO
import com.deathhit.data.media_progress.MediaProgressRepository
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item.model.toMediaItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NavigationActivityViewModel @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaProgressRepository: MediaProgressRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "NavigationActivityViewModel"
        private const val KEY_IS_FIRST_FRAME_RENDERED = "$TAG.KEY_IS_FIRST_FRAME_RENDERED"
        private const val KEY_IS_FOR_TAB_TO_PLAY = "$TAG.KEY_IS_FOR_TAB_TO_PLAY"
        private const val KEY_IS_PLAYER_VIEW_EXPANDED = "$TAG.KEY_IS_PLAYER_VIEW_EXPANDED"
        private const val KEY_PLAY_ITEM_ID = "$TAG.KEY_PLAY_ITEM_ID"
        private const val KEY_TAB = "$TAG.KEY_TAB"
    }

    data class State(
        val actions: List<Action>,
        val attachedTabs: Set<Tab>,
        val isFirstFrameRendered: Boolean,
        val isForTabToPlay: Boolean,
        val isPlayerConnected: Boolean,
        val isPlayerViewExpanded: Boolean,
        val playItem: MediaItemVO?,
        val playItemId: String?,
        val tab: Tab
    ) {
        sealed interface Action {
            object CollapsePlayerView : Action
            object ExpandPlayerView : Action
            object HidePlayerView : Action
            object HideSystemBars : Action
            object PausePlayback : Action
            object PlayPlayback : Action
            data class PreparePlayback(
                val isEnded: Boolean,
                val position: Long,
                val sourceUrl: String
            ) : Action

            object ShowPlayerViewControls : Action
            object ShowSystemBars : Action
            object StopPlayback : Action
        }

        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }

        val isPlayingByPlayerView = !isForTabToPlay && isPlayerConnected
        val playTab =
            if (attachedTabs.contains(tab) && isForTabToPlay && isPlayerConnected) tab else null
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                attachedTabs = emptySet(),
                isFirstFrameRendered = savedStateHandle[KEY_IS_FIRST_FRAME_RENDERED] ?: false,
                isForTabToPlay = savedStateHandle[KEY_IS_FOR_TAB_TO_PLAY] ?: true,
                isPlayerConnected = false,
                isPlayerViewExpanded = savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] ?: false,
                playItem = null,
                playItemId = savedStateHandle[KEY_PLAY_ITEM_ID],
                tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val isFirstFrameRendered get() = stateFlow.value.isFirstFrameRendered
    private val isForTabToPlay get() = stateFlow.value.isForTabToPlay
    private val isPlayerViewExpanded get() = stateFlow.value.isPlayerViewExpanded
    private val isPlayingByPlayerView get() = stateFlow.value.isPlayingByPlayerView
    private val playItemId get() = stateFlow.value.playItemId
    private val tab get() = stateFlow.value.tab

    private val playItemFlow: Flow<MediaItemVO?> =
        stateFlow.map { it.playItemId }.distinctUntilChanged().flatMapLatest { playItemId ->
            playItemId?.let {
                mediaItemRepository.getMediaItemFlowById(playItemId).map { it?.toMediaItemVO() }
            } ?: flowOf(null)
        }

    private var prepareItemJob: Job? = null

    init {
        viewModelScope.launch {
            playItemFlow.distinctUntilChanged().collectLatest {
                _stateFlow.update { state ->
                    state.copy(playItem = it)
                }
            }
        }
    }

    fun addAttachedTab(tab: State.Tab) {
        _stateFlow.update { state ->
            state.copy(
                attachedTabs = state.attachedTabs + tab
            )
        }
    }

    fun clearPlayerViewPlayback() {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.HidePlayerView,
                isForTabToPlay = true
            )
        }

        prepareItem(null)
    }

    fun collapsePlayerView() {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.CollapsePlayerView
            )
        }
    }

    fun notifyFirstFrameRendered() {
        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = true)
        }
    }

    fun onAction(action: State.Action) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions - action)
        }
    }

    fun openItem(itemId: String) {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.ExpandPlayerView + State.Action.PlayPlayback,
                isForTabToPlay = false,
                playItemId = null   //Clear previous content to prevent seeing it
            )
        }

        prepareItem(itemId)
    }

    fun pausePlayerViewPlayback() {
        if (!isPlayingByPlayerView)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PausePlayback)
        }
    }

    fun resumePlayerViewPlayback() {
        if (!isPlayingByPlayerView)
            return

        prepareItem(playItemId)
    }

    fun savePlayItemPosition(isEnded: Boolean, mediaPosition: Long) {
        playItemId?.let {
            viewModelScope.launch(NonCancellable) {
                mediaProgressRepository.setMediaProgress(
                    MediaProgressDO(
                        isEnded,
                        it,
                        mediaPosition
                    )
                )
            }
        }
    }

    fun saveState() {
        savedStateHandle[KEY_IS_FIRST_FRAME_RENDERED] = isFirstFrameRendered
        savedStateHandle[KEY_IS_FOR_TAB_TO_PLAY] = isForTabToPlay
        savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] = isPlayerViewExpanded
        savedStateHandle[KEY_PLAY_ITEM_ID] = playItemId
        savedStateHandle[KEY_TAB] = tab
    }

    fun setIsPlayerConnected(isPlayerConnected: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlayerConnected = isPlayerConnected)
        }
    }

    fun setIsPlayerViewExpanded(isPlayerViewExpanded: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlayerViewExpanded = isPlayerViewExpanded)
        }
    }

    fun setTab(tab: State.Tab) {
        if (tab == this.tab)
            return

        _stateFlow.update { state ->
            state.copy(tab = tab)
        }
    }

    fun showPlayerViewControls() {
        if (!isPlayingByPlayerView)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ShowPlayerViewControls)
        }
    }

    fun toggleSystemBars(isLandscape: Boolean) {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + if (isLandscape && isPlayerViewExpanded)
                    State.Action.HideSystemBars
                else
                    State.Action.ShowSystemBars
            )
        }
    }

    private fun prepareItem(itemId: String?) {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.StopPlayback, isFirstFrameRendered = false
            )
        }

        prepareItemJob?.cancel()
        prepareItemJob = viewModelScope.launch {
            yield() //Allow the assignment of playItemId to be canceled.

            _stateFlow.update { state ->
                state.copy(playItemId = itemId)
            }

            val playItem = playItemFlow.first()

            if (playItem != null)
                _stateFlow.update { state ->
                    val progress =
                        mediaProgressRepository.getMediaProgressByMediaItemId(playItem.id)

                    state.copy(
                        actions = state.actions + State.Action.PreparePlayback(
                            progress?.isEnded ?: false,
                            progress?.position ?: 0L,
                            playItem.sourceUrl
                        )
                    )
                }
        }
    }
}
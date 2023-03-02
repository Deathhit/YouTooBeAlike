package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_item.model.MediaItemDO
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
        private const val KEY_IS_PLAYER_VIEW_EXPANDED = "$TAG.KEY_IS_PLAYER_VIEW_EXPANDED"
        private const val KEY_IS_PLAYING_BY_TAB = "$TAG.KEY_IS_PLAYING_BY_TAB"
        private const val KEY_PLAY_ITEM_ID = "$TAG.KEY_PLAY_ITEM_ID"
        private const val KEY_TAB = "$TAG.KEY_TAB"

        private const val MEDIA_SWITCHING_DELAY = 500L
    }

    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isPlayerConnected: Boolean,
        val isPlayerViewExpanded: Boolean,
        val isPlayingByTab: Boolean,
        val pendingPlayItemId: String?,
        val playItem: MediaItemVO?,
        val playItemId: String?,
        val tab: Tab
    ) {
        sealed interface Action {
            object CollapsePlayerView : Action
            object ExpandPlayerView : Action
            object HidePlayerView : Action
            object PauseMedia : Action
            object PlayMedia : Action
            data class PrepareMedia(
                val isEnded: Boolean,
                val position: Long,
                val sourceUrl: String
            ) : Action

            object SetPlayerToTab : Action
            object ShowPlayerViewControls : Action
            object StopMedia : Action
        }

        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }

        val isPlayingByPlayerView = isPlayerConnected && !isPlayingByTab
        val playingTab = if (isPlayingByTab) tab else null
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = false,
                isPlayerConnected = false,
                isPlayerViewExpanded = savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] ?: false,
                isPlayingByTab = savedStateHandle[KEY_IS_PLAYING_BY_TAB] ?: true,
                pendingPlayItemId = null,
                playItem = null,
                playItemId = savedStateHandle[KEY_PLAY_ITEM_ID],
                tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val isPlayerViewExpanded get() = stateFlow.value.isPlayerViewExpanded
    private val isPlayingByTab get() = stateFlow.value.isPlayingByTab
    private val pendingPlayItemId get() = stateFlow.value.pendingPlayItemId
    private val playItemId get() = stateFlow.value.playItemId
    private val tab get() = stateFlow.value.tab

    private val playItemDOFlow: Flow<MediaItemDO?> =
        stateFlow.map { it.playItemId }.distinctUntilChanged().flatMapLatest { playItemId ->
            playItemId?.let { mediaItemRepository.getMediaItemFlowById(it) } ?: flowOf(null)
        }

    private var prepareItemJob: Job? = null

    init {
        viewModelScope.launch {
            playItemDOFlow.map { it?.toMediaItemVO() }.collectLatest {
                _stateFlow.update { state ->
                    state.copy(playItem = it)
                }
            }
        }
    }

    fun clearItem() {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.HidePlayerView,
                isPlayingByTab = true
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

    fun onAction(action: State.Action) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions - action)
        }
    }

    fun openItem(itemId: String) {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.ExpandPlayerView + State.Action.PlayMedia,
                isPlayingByTab = false
            )
        }

        prepareItem(null)
        prepareItem(itemId)
    }

    fun pausePlayerViewPlayback() {
        if (isPlayingByTab)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PauseMedia)
        }
    }

    fun prepareItemAndPlay(itemId: String?) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PlayMedia)
        }

        prepareItem(itemId)
    }

    fun resumePlayerViewPlayback() {
        if (isPlayingByTab)
            return

        val itemId = playItemId

        prepareItem(null)
        prepareItem(itemId)
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
        savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] = isPlayerViewExpanded
        savedStateHandle[KEY_IS_PLAYING_BY_TAB] = isPlayingByTab
        savedStateHandle[KEY_PLAY_ITEM_ID] = playItemId
        savedStateHandle[KEY_TAB] = tab
    }

    fun setIsFirstFrameRendered(isFirstFrameRendered: Boolean) {
        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = isFirstFrameRendered)
        }
    }

    fun setIsPlayerConnected(isPlayerConnected: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlayerConnected = isPlayerConnected)
        }

        if (isPlayerConnected)
            setPlayerToTab()
    }

    fun setIsPlayerViewExpanded(isPlayerViewExpanded: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlayerViewExpanded = isPlayerViewExpanded)
        }
    }

    fun setPlayerToTab() {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.SetPlayerToTab)
        }
    }

    fun setTab(tab: State.Tab) {
        if (tab == this.tab)
            return

        _stateFlow.update { state ->
            state.copy(tab = tab)
        }

        if (isPlayingByTab)
            prepareItem(null)
    }

    fun showPlayerViewControls() {
        if (isPlayingByTab)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ShowPlayerViewControls)
        }
    }

    private fun prepareItem(itemId: String?) {
        if (itemId == pendingPlayItemId)
            return

        //Stop the player before a new item is ready.
        //Only set pendingPlayItem first to allow time to save media progress.
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.StopMedia, pendingPlayItemId = itemId)
        }

        prepareItemJob?.cancel()
        prepareItemJob = viewModelScope.launch {
            yield() //Allow the assignment of playItemId to be canceled.

            _stateFlow.update { state ->
                state.copy(playItemId = itemId)
            }

            delay(MEDIA_SWITCHING_DELAY)

            val itemDO = playItemDOFlow.first()

            if (itemDO != null)
                _stateFlow.update { state ->
                    val progress =
                        mediaProgressRepository.getMediaProgressByMediaItemId(itemDO.mediaItemId)

                    state.copy(
                        actions = state.actions + State.Action.PrepareMedia(
                            progress?.isEnded ?: false,
                            progress?.position ?: 0L,
                            itemDO.sourceUrl
                        )
                    )
                }
        }
    }
}
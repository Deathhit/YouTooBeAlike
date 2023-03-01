package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.data.media_progress.MediaProgressDO
import com.deathhit.data.media_progress.repository.MediaProgressRepository
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item.model.toMediaItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class NavigationActivityViewModel @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaProgressRepository: MediaProgressRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "NavigationActivityViewModel"
        private const val KEY_IS_PLAYER_VIEW_EXPANDED = "$TAG.KEY_IS_PLAYER_VIEW_EXPANDED"
        private const val KEY_IS_PLAYING_BY_TAB_PAGE = "$TAG.KEY_IS_PLAYING_BY_TAB_PAGE"
        private const val KEY_TAB = "$TAG.KEY_TAB"

        private const val MEDIA_SWITCHING_DELAY = 500L
    }

    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isPlayerConnected: Boolean,
        val isPlayerViewExpanded: Boolean,
        val isPlayingByTabPage: Boolean,
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

            object StopMedia : Action
        }

        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }

        val isPlayingByPlayerView = isPlayerConnected && !isPlayingByTabPage
        val playingTab = if (isPlayingByTabPage) tab else null
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = false,
                isPlayerConnected = false,
                isPlayerViewExpanded = savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] ?: false,
                isPlayingByTabPage = savedStateHandle[KEY_IS_PLAYING_BY_TAB_PAGE] ?: true,
                pendingPlayItemId = null,
                playItem = null,
                playItemId = null,
                tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val isPlayerViewExpanded get() = stateFlow.value.isPlayerViewExpanded
    private val isPlayingByTabPage get() = stateFlow.value.isPlayingByTabPage
    private val pendingPlayItemId get() = stateFlow.value.pendingPlayItemId
    private val playItemId get() = stateFlow.value.playItemId
    private val tab get() = stateFlow.value.tab

    private var prepareItemJob: Job? = null

    fun clearItem() {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.HidePlayerView,
                isPlayingByTabPage = true
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
                actions = state.actions + State.Action.ExpandPlayerView,
                isPlayingByTabPage = false
            )
        }

        prepareItem(null)
        prepareItem(itemId)
    }

    fun pauseMedia() {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PauseMedia)
        }
    }

    fun prepareItem(itemId: String?) {
        if (itemId == pendingPlayItemId)
            return

        //Stop the player before a new item is ready.
        //Only set pendingPlayItem first to allow time to save media progress.
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.StopMedia, pendingPlayItemId = itemId)
        }

        prepareItemJob?.cancel()
        prepareItemJob = viewModelScope.launch {
            val itemDO = itemId?.let { mediaItemRepository.getMediaItemFlowById(it) }?.first()

            _stateFlow.update { state ->
                state.copy(playItem = itemDO?.toMediaItemVO())
            }

            delay(MEDIA_SWITCHING_DELAY)

            if(itemDO != null)
                _stateFlow.update { state ->
                    val progress =
                        mediaProgressRepository.getMediaProgressByMediaItemId(itemId)

                    state.copy(
                        actions = state.actions + State.Action.PlayMedia + State.Action.PrepareMedia(
                            progress?.isEnded ?: false,
                            progress?.position ?: 0L,
                            itemDO.sourceUrl
                        )
                    )
                }

            _stateFlow.update { state ->
                state.copy(playItemId = itemId)
            }
        }
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
        savedStateHandle[KEY_IS_PLAYING_BY_TAB_PAGE] = isPlayingByTabPage
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

        if (isPlayingByTabPage)
            prepareItem(null)
    }
}
package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_progress.model.MediaProgressDO
import com.deathhit.data.media_progress.MediaProgressRepository
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
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
        val isViewInForeground: Boolean,
        val isViewInLandscape: Boolean,
        val playItem: MediaItemVO?,
        val playItemId: String?,
        val requestedScreenOrientation: ScreenOrientation,
        val tab: Tab
    ) {
        sealed interface Action {
            object CollapsePlayerView : Action
            object ExpandPlayerView : Action
            object HidePlayerView : Action
            object PausePlayback : Action
            object PlayPlayback : Action
            data class PreparePlayback(
                val isEnded: Boolean,
                val mediaItemId: String,
                val position: Long,
                val sourceUrl: String
            ) : Action

            object ShowPlayerViewControls : Action
            object StopPlayback : Action
        }

        enum class ScreenOrientation {
            LANDSCAPE,
            PORTRAIT,
            UNSPECIFIED
        }

        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }

        val isFullscreen = isPlayerViewExpanded && isViewInLandscape
        val isMediaSessionActive = if (isPlayerConnected) isViewInForeground else null
        val isPlayingByPlayerView = !isForTabToPlay && isPlayerConnected
        val playTab =
            if (attachedTabs.contains(tab) && isForTabToPlay && isPlayerConnected) tab else null
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                attachedTabs = emptySet(),
                isFirstFrameRendered = false,
                isForTabToPlay = savedStateHandle[KEY_IS_FOR_TAB_TO_PLAY] ?: true,
                isPlayerConnected = false,
                isPlayerViewExpanded = savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] ?: false,
                isViewInForeground = false,
                isViewInLandscape = false,
                playItem = null,
                playItemId = savedStateHandle[KEY_PLAY_ITEM_ID],
                requestedScreenOrientation = State.ScreenOrientation.UNSPECIFIED,
                tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val isForTabToPlay get() = stateFlow.value.isForTabToPlay
    private val isFullscreen get() = stateFlow.value.isFullscreen
    private val isPlayerConnected get() = stateFlow.value.isPlayerConnected
    private val isPlayerViewExpanded get() = stateFlow.value.isPlayerViewExpanded
    private val isPlayingByPlayerView get() = stateFlow.value.isPlayingByPlayerView
    private val isViewInLandscape get() = stateFlow.value.isViewInLandscape
    private val playItemId get() = stateFlow.value.playItemId
    private val requestedScreenOrientation get() = stateFlow.value.requestedScreenOrientation
    private val tab get() = stateFlow.value.tab

    private val playItemFlow: Flow<MediaItemVO?> =
        stateFlow.map { it.playItemId }.distinctUntilChanged().flatMapLatest { playItemId ->
            playItemId?.let {
                mediaItemRepository.getMediaItemFlowById(playItemId).map { it?.toMediaItemVO() }
            } ?: flowOf(null)
        }

    private var prepareItemJob: Job? = null
    private var unlockScreenOrientationJob: Job? = null

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
                actions = state.actions + State.Action.HidePlayerView + State.Action.StopPlayback,
                isFirstFrameRendered = false,
                isForTabToPlay = true,
                playItemId = null
            )
        }

        prepareItemJob?.cancel()
    }

    fun collapsePlayerView() {
        if (isFullscreen)
            toggleScreenOrientation()
        else
            _stateFlow.update { state ->
                state.copy(
                    actions = state.actions + State.Action.CollapsePlayerView
                )
            }
    }

    fun notifyFirstFrameRendered() {
        if (!isPlayingByPlayerView)
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

    fun openItem(itemId: String) {
        if (!isPlayerConnected)
            return

        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.ExpandPlayerView + State.Action.PlayPlayback,
                isForTabToPlay = false,
                playItemId = null   //Clear previous value first to prevent UI glitching.
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

        playItemId?.let { prepareItem(it) }
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

    fun setIsViewInForeground(isViewInForeground: Boolean) {
        _stateFlow.update { state ->
            state.copy(isViewInForeground = isViewInForeground)
        }
    }

    fun setIsViewInLandscape(isViewInLandscape: Boolean) {
        _stateFlow.update { state ->
            state.copy(isViewInLandscape = isViewInLandscape)
        }
    }

    fun setTab(tab: State.Tab) {
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

    fun toggleScreenOrientation() {
        _stateFlow.update { state ->
            state.copy(requestedScreenOrientation = if (isViewInLandscape) State.ScreenOrientation.PORTRAIT else State.ScreenOrientation.LANDSCAPE)
        }
    }

    fun unlockScreenOrientation(orientation: Int) {
        if (requestedScreenOrientation == State.ScreenOrientation.LANDSCAPE || requestedScreenOrientation == State.ScreenOrientation.UNSPECIFIED)
            return

        unlockScreenOrientationJob?.cancel()
        unlockScreenOrientationJob = viewModelScope.launch {
            delay(200)

            val epsilon = 15
            val isPortraitOrientation = (orientation >= 360 - epsilon || orientation <= epsilon)

            if (!isViewInLandscape && isPortraitOrientation && requestedScreenOrientation == State.ScreenOrientation.PORTRAIT)
                _stateFlow.update { state ->
                    state.copy(requestedScreenOrientation = State.ScreenOrientation.UNSPECIFIED)
                }
        }
    }

    private fun prepareItem(itemId: String) {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.StopPlayback, isFirstFrameRendered = false
            )
        }

        prepareItemJob?.cancel()
        prepareItemJob = viewModelScope.launch {
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
                            playItem.id,
                            progress?.position ?: 0L,
                            playItem.sourceUrl
                        )
                    )
                }
        }
    }
}
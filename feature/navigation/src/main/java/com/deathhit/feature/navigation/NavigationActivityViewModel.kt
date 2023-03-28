package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.model.MediaProgressDO
import com.deathhit.domain.MediaProgressRepository
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
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
        private const val KEY_PLAY_ITEM_ID = "$TAG.KEY_PLAY_ITEM_ID"
        private const val KEY_TAB = "$TAG.KEY_TAB"
    }

    data class State(
        val actions: List<Action>,
        val attachedPages: Set<Page>,
        val currentPage: Page,
        val isFirstFrameRendered: Boolean,
        val isPlayerConnected: Boolean,
        val isPlayerViewExpanded: Boolean,
        val isViewInForeground: Boolean,
        val isViewInLandscape: Boolean,
        val playItem: MediaItemVO?,
        val playItemId: String?,
        val requestedScreenOrientation: ScreenOrientation
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
                val position: Long?,
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

        enum class Page {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }

        val isFullscreen = isPlayerViewExpanded && isViewInLandscape
        val isMediaSessionActive = if (isPlayerConnected) isViewInForeground else null
        val isPlayingByPlayerView = isPlayerConnected && playItemId != null
        val playPage =
            if (attachedPages.contains(currentPage) && isPlayerConnected && playItemId == null) currentPage else null
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                attachedPages = emptySet(),
                currentPage = savedStateHandle[KEY_TAB] ?: State.Page.HOME,
                isFirstFrameRendered = false,
                isPlayerConnected = false,
                isPlayerViewExpanded = savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] ?: false,
                isViewInForeground = false,
                isViewInLandscape = false,
                playItem = null,
                playItemId = savedStateHandle[KEY_PLAY_ITEM_ID],
                requestedScreenOrientation = State.ScreenOrientation.UNSPECIFIED
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val currentPage get() = stateFlow.value.currentPage
    private val isFullscreen get() = stateFlow.value.isFullscreen
    private val isPlayerConnected get() = stateFlow.value.isPlayerConnected
    private val isPlayerViewExpanded get() = stateFlow.value.isPlayerViewExpanded
    private val isPlayingByPlayerView get() = stateFlow.value.isPlayingByPlayerView
    private val isViewInLandscape get() = stateFlow.value.isViewInLandscape
    private val playItemId get() = stateFlow.value.playItemId
    private val requestedScreenOrientation get() = stateFlow.value.requestedScreenOrientation

    private var prepareItemJob: Job? = null
    private var unlockScreenOrientationJob: Job? = null

    init {
        viewModelScope.launch {
            launch {
                stateFlow.filter { it.isPlayerConnected }.map { it.playItemId }.distinctUntilChanged()
                    .collectLatest {
                        _stateFlow.update { state ->
                            state.copy(
                                actions = state.actions + State.Action.StopPlayback,
                                isFirstFrameRendered = false
                            )
                        }

                        prepareItemJob?.cancel()

                        if (it == null) {
                            _stateFlow.update { state ->
                                state.copy(playItem = null)
                            }

                            return@collectLatest
                        }

                        prepareItemJob = launch {
                            prepareItem(this, it)
                        }
                    }
            }
        }
    }

    fun clearPlayerViewPlayback() {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.HidePlayerView,
                playItemId = null
            )
        }
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

    fun notifyFirstFrameRendered(mediaItemId: String) {
        if (mediaItemId != playItemId)
            return

        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = true)
        }
    }

    fun notifyPageAttached(page: State.Page) {
        _stateFlow.update { state ->
            state.copy(
                attachedPages = state.attachedPages + page
            )
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
                playItemId = itemId
            )
        }
    }

    fun pausePlayerViewPlayback() {
        if (!isPlayingByPlayerView)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PausePlayback)
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
        savedStateHandle[KEY_IS_PLAYER_VIEW_EXPANDED] = isPlayerViewExpanded
        savedStateHandle[KEY_PLAY_ITEM_ID] = playItemId
        savedStateHandle[KEY_TAB] = currentPage
    }

    fun setCurrentPage(page: State.Page) {
        _stateFlow.update { state ->
            state.copy(currentPage = page)
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

    private suspend fun prepareItem(coroutineScope: CoroutineScope, playItemId: String) =
        with(coroutineScope) {
            val playItemDO = mediaItemRepository.getMediaItemFlowById(playItemId)
                .distinctUntilChanged().stateIn(this).run {
                    launch {
                        collectLatest {
                            _stateFlow.update { state ->
                                state.copy(playItem = it?.toMediaItemVO())
                            }
                        }
                    }

                    value
                }

            if (playItemDO != null)
                _stateFlow.update { state ->
                    val progress =
                        mediaProgressRepository.getMediaProgressByMediaItemId(playItemId)

                    state.copy(
                        actions = state.actions + State.Action.PreparePlayback(
                            progress?.isEnded ?: false,
                            playItemId,
                            progress?.position,
                            playItemDO.sourceUrl
                        )
                    )
                }
        }
}
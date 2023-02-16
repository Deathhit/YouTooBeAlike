package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.data.media_progress.MediaProgressDO
import com.deathhit.data.media_progress.repository.MediaProgressRepository
import com.deathhit.feature.media_item.model.ItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationActivityViewModel @Inject constructor(
    private val mediaProgressRepository: MediaProgressRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "HomeActivityViewModel"
        private const val KEY_TAB = "$TAG.KEY_TAB"

        private const val MEDIA_SWITCHING_DELAY = 500L
    }

    data class State(
        val actions: List<Action>,
        val playItem: ItemVO?,
        val tab: Tab
    ) {
        sealed interface Action {
            data class PrepareMedia(val item: ItemVO, val position: Long) : Action
            object StopPlayer : Action
        }

        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                playItem = null,
                tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val playItem get() = stateFlow.value.playItem
    private val tab get() = stateFlow.value.tab

    private var prepareItemJob: Job? = null

    fun preparePlayItem(playItem: ItemVO?) {
        if (playItem == this.playItem)
            return

        _stateFlow.update { state ->
            state.copy(playItem = playItem)
        }

        prepareItemJob?.cancel()
        prepareItemJob = viewModelScope.launch {
            if (playItem == null)
                return@launch

            delay(MEDIA_SWITCHING_DELAY)

            _stateFlow.update { state ->
                state.copy(
                    actions = state.actions + State.Action.PrepareMedia(
                        playItem,
                        mediaProgressRepository.getMediaProgressBySourceUrl(playItem.sourceUrl)?.position
                            ?: 0L
                    )
                )
            }
        }
    }

    fun savePlayItemPosition(mediaPosition: Long) {
        playItem?.let {
            viewModelScope.launch(NonCancellable) {
                mediaProgressRepository.setMediaProgress(
                    MediaProgressDO(
                        mediaPosition,
                        it.sourceUrl
                    )
                )
            }
        }
    }

    fun saveState() {
        savedStateHandle[KEY_TAB] = tab
    }

    fun setTab(tab: State.Tab) {
        _stateFlow.update { state ->
            state.copy(tab = tab)
        }
    }

    fun stopPlayer() {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.StopPlayer)
        }
    }
}
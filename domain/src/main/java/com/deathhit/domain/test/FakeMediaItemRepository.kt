package com.deathhit.domain.test

import androidx.paging.PagingData
import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.enum_type.MediaItemLabel
import com.deathhit.domain.model.MediaItemDO
import kotlinx.coroutines.flow.*

class FakeMediaItemRepository : MediaItemRepository {
    data class State(val actions: List<Action>) {
        sealed interface Action {
            data class ClearByLabel(val mediaItemLabel: MediaItemLabel) : Action
            data class GetMediaItemFlowById(val mediaItemId: String) : Action
            data class GetMediaItemPagingDataFlow(
                val exclusiveId: String?,
                val mediaItemLabel: MediaItemLabel,
                val subtitle: String?
            ) : Action
        }
    }

    private val _stateFlow = MutableStateFlow(State(actions = emptyList()))
    val stateFlow = _stateFlow.asStateFlow()

    var funcClearByLabel: (suspend (mediaItemLabel: MediaItemLabel) -> Unit)? = null
    var funcGetMediaItemFlowById: ((mediaItemId: String) -> Flow<MediaItemDO?>)? = null
    var funcGetMediaItemPagingDataFlow: ((
        exclusiveId: String?,
        mediaItemLabel: MediaItemLabel,
        subtitle: String?
    ) -> Flow<PagingData<MediaItemDO>>)? = null

    override suspend fun clearByLabel(mediaItemLabel: MediaItemLabel) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ClearByLabel(mediaItemLabel))
        }

        funcClearByLabel?.invoke(mediaItemLabel)
    }

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaItemFlowById(mediaItemId))
        }

        return funcGetMediaItemFlowById?.invoke(mediaItemId) ?: flowOf(null)
    }

    override fun getMediaItemPagingDataFlow(
        exclusiveId: String?,
        mediaItemLabel: MediaItemLabel,
        subtitle: String?
    ): Flow<PagingData<MediaItemDO>> {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.GetMediaItemPagingDataFlow(
                    exclusiveId,
                    mediaItemLabel,
                    subtitle
                )
            )
        }

        return funcGetMediaItemPagingDataFlow?.invoke(exclusiveId, mediaItemLabel, subtitle)
            ?: flowOf(PagingData.empty())
    }
}
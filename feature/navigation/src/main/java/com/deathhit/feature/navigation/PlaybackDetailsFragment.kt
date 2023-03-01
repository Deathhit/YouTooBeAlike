package com.deathhit.feature.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.deathhit.feature.media_item.adapter.media_item.MediaItemAdapter
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.navigation.databinding.FragmentPlaybackDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaybackDetailsFragment : Fragment() {
    companion object {
        private const val TAG = "PlaybackDetailsFragment"
        private const val KEY_MOTION_TRANSITION_STATE = "$TAG.KEY_MOTION_TRANSITION_STATE"

        fun create() = PlaybackDetailsFragment()
    }

    interface Callback {
        fun onOpenPlayableItem(item: MediaItemVO)
    }

    var callback: Callback? = null

    private val binding get() = _binding!!
    private var _binding: FragmentPlaybackDetailsBinding? = null

    private val viewModel: PlaybackDetailsViewModel by viewModels()

    private val glideRequestManager get() = _glideRequestManager!!
    private var _glideRequestManager: RequestManager? = null

    private val playableItemAdapter get() = _playableItemAdapter!!
    private var _playableItemAdapter: MediaItemAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentPlaybackDetailsBinding.inflate(inflater, container, false)
        .also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _glideRequestManager = Glide.with(this)

        _playableItemAdapter = object : MediaItemAdapter(glideRequestManager) {
            override fun onBindPlayPosition(item: MediaItemVO) {

            }

            override fun onClickItem(item: MediaItemVO) {
                viewModel.openPlayableItem(item)
            }
        }

        with(binding.recyclerView) {
            adapter = playableItemAdapter
            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.actions }.distinctUntilChanged()
                        .collect { actions ->
                            actions.forEach { action ->
                                when (action) {
                                    is PlaybackDetailsViewModel.State.Action.OpenPlayableItem -> callback?.onOpenPlayableItem(
                                        action.item
                                    )
                                    PlaybackDetailsViewModel.State.Action.ResetView -> {
                                        //todo
                                        //binding.motionLayout.transitionToStart()
                                        //binding.recyclerView.adapter = playableItemAdapter
                                    }
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.playableItem }.distinctUntilChanged().collect {
                        with(binding.textViewDescription) {
                            text = it?.description
                        }

                        with(binding.textViewSubtitle) {
                            text = it?.subtitle
                        }

                        with(binding.textViewTitle) {
                            text = it?.title
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendedItemPagingDataFlow.collectLatest {
                playableItemAdapter.submitData(lifecycle, it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null

        _binding = null

        _glideRequestManager = null

        _playableItemAdapter = null
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        with(binding.motionLayout) {
            transitionState = savedInstanceState?.getBundle(KEY_MOTION_TRANSITION_STATE) ?: transitionState
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        with(binding.motionLayout) {
            outState.putBundle(KEY_MOTION_TRANSITION_STATE, transitionState)
        }

        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    fun setPlayableItemId(playableItemId: String?) {
        viewModel.setPlayableItemId(playableItemId)
    }
}
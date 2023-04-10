package com.deathhit.feature.playback_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.deathhit.feature.media_item_list.MediaItemLoadStateAdapter
import com.deathhit.feature.media_item_list.MediaItemPagingDataAdapter
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.playback_details.databinding.FragmentPlaybackDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaybackDetailsFragment : Fragment() {
    companion object {
        fun create() = PlaybackDetailsFragment()
    }

    interface Callback {
        fun onOpenItem(itemId: String)
    }

    var callback: Callback? = null

    private val binding get() = _binding!!
    private var _binding: FragmentPlaybackDetailsBinding? = null

    private val viewModel: PlaybackDetailsViewModel by viewModels()

    private val glideRequestManager get() = _glideRequestManager!!
    private var _glideRequestManager: RequestManager? = null

    private val playbackDetailsAdapter get() = _playbackDetailsAdapter!!
    private var _playbackDetailsAdapter: PlaybackDetailsListAdapter? = null

    private val recommendedItemAdapter get() = _recommendedItemAdapter!!
    private var _recommendedItemAdapter: MediaItemPagingDataAdapter? = null

    private var loadPlaybackDetailsJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentPlaybackDetailsBinding.inflate(inflater, container, false)
        .also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _glideRequestManager = Glide.with(this)

        _recommendedItemAdapter = object : MediaItemPagingDataAdapter(glideRequestManager) {
            override fun onBindPlayPosition(item: MediaItemVO) {}

            override fun onClickItem(item: MediaItemVO) {
                viewModel.openItem(item)
            }
        }

        _playbackDetailsAdapter = PlaybackDetailsListAdapter()

        with(binding.recyclerView) {
            adapter = ConcatAdapter(
                playbackDetailsAdapter,
                recommendedItemAdapter.withLoadStateFooter(object :
                    MediaItemLoadStateAdapter() {
                    override fun onRetryLoading() {
                        viewModel.retryLoadingRecommendedList()
                    }
                })
            )

            setHasFixedSize(true)
        }

        bindViewModelState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null

        _binding = null

        _glideRequestManager = null

        _playbackDetailsAdapter = null

        _recommendedItemAdapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    fun loadPlaybackDetails(playItemId: String?) {
        loadPlaybackDetailsJob?.cancel()
        loadPlaybackDetailsJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.loadPlaybackDetails(playItemId)
            }
        }
    }

    private fun bindViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.actions }.distinctUntilChanged()
                        .collect { actions ->
                            actions.forEach { action ->
                                when (action) {
                                    is PlaybackDetailsViewModel.State.Action.OpenItem -> callback?.onOpenItem(
                                        action.item.id
                                    )
                                    PlaybackDetailsViewModel.State.Action.RetryLoadingRecommendedList -> recommendedItemAdapter.retry()
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.playbackDetails }.distinctUntilChanged().collect {
                        playbackDetailsAdapter.submitList(listOf(it))
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendedItemPagingDataFlow.collectLatest {
                recommendedItemAdapter.submitData(lifecycle, it)
            }
        }
    }
}
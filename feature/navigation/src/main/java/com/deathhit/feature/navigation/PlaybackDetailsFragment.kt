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
import androidx.recyclerview.widget.ConcatAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.deathhit.core.ui.AppLoadStateAdapter
import com.deathhit.feature.media_item.MediaItemAdapter
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

    private val playableItemAdapter get() = _playableItemAdapter!!
    private var _playableItemAdapter: MediaItemAdapter? = null

    private val playbackDetailsAdapter get() = _playbackDetailsAdapter!!
    private var _playbackDetailsAdapter: PlaybackDetailsAdapter? = null

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
                viewModel.openItem(item)
            }
        }

        _playbackDetailsAdapter = PlaybackDetailsAdapter()

        with(binding.recyclerView) {
            adapter = ConcatAdapter(
                playbackDetailsAdapter,
                playableItemAdapter.withLoadStateFooter(object :
                    AppLoadStateAdapter() {
                    override fun onRetryLoading() {
                        playableItemAdapter.retry()
                    }
                })
            )

            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.actions }.distinctUntilChanged()
                        .collect { actions ->
                            actions.forEach { action ->
                                when (action) {
                                    is PlaybackDetailsViewModel.State.Action.OpenItem -> callback?.onOpenItem(
                                        action.itemId
                                    )
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

        _playbackDetailsAdapter = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    fun setPlayableItemId(playableItemId: String?) {
        viewModel.setPlayItemId(playableItemId)
    }
}
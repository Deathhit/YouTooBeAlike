package com.deathhit.feature.media_item.fragment.media_item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deathhit.core.ui.adapter.load_state.LoadStateAdapter
import com.deathhit.feature.media_item.adapter.media_item.MediaItemAdapter
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item_list.databinding.FragmentMediaItemListBinding
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MediaItemListFragment : Fragment() {
    companion object {
        fun create() = MediaItemListFragment()
    }

    interface Callback {
        fun onOpenItem(item: MediaItemVO)
        fun onPrepareItem(item: MediaItemVO?)
    }

    var callback: Callback? = null

    var player: Player? = null
        set(value) {
            field?.removeListener(playerListener)
            field = value?.apply { addListener(playerListener) }
            _mediaItemAdapter?.setPlayer(player)
        }

    private val binding get() = _binding!!
    private var _binding: FragmentMediaItemListBinding? = null

    private val viewModel: MediaItemListViewModel by viewModels()

    private val linearLayoutManager get() = _linearLayoutManger!!
    private var _linearLayoutManger: LinearLayoutManager? = null
    private val playPosition
        get() = linearLayoutManager.findFirstCompletelyVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION)
                null
            else
                it
        }

    private val mediaItemAdapter get() = _mediaItemAdapter!!
    private var _mediaItemAdapter: MediaItemAdapter? = null

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            viewModel.setPlayPosition(playPosition)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            viewModel.notifyFirstFrameRendered()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMediaItemListBinding.inflate(inflater, container, false).run {
        _binding = this
        root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            setHasFixedSize(true)

            _linearLayoutManger = (layoutManager!! as LinearLayoutManager)

            _mediaItemAdapter = object : MediaItemAdapter() {
                override fun onBindPlayPosition(item: MediaItemVO) {
                    viewModel.preparePlayItem(item)
                }

                override fun onClickItem(item: MediaItemVO) {
                    viewModel.openItem(item)
                }
            }.apply { setPlayer(player) }.also {
                adapter =
                    it.apply {
                        addOnPagesUpdatedListener {
                            if (itemCount > 0)
                                viewModel.scrollToTopOnFirstPageLoaded()
                        }
                    }.withLoadStateFooter(object : LoadStateAdapter() {
                        override fun onRetryLoading() {
                            mediaItemAdapter.retry()
                        }
                    })
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.actions }.distinctUntilChanged()
                        .collect { actions ->
                            actions.forEach { action ->
                                when (action) {
                                    is MediaItemListViewModel.State.Action.OpenItem -> callback?.onOpenItem(
                                        action.item
                                    )
                                    is MediaItemListViewModel.State.Action.PrepareItem -> callback?.onPrepareItem(
                                        action.item
                                    )
                                    MediaItemListViewModel.State.Action.ScrollToTop -> binding.recyclerView.scrollToPosition(
                                        0
                                    )
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.isFirstFrameRendered }.distinctUntilChanged()
                        .collect {
                            //The Runnable has the potential to outlive the viewLifecycleScope,
                            // so we use an extra launch{} to make sure it only runs in the scope.
                            binding.recyclerView.post {
                                launch {
                                    mediaItemAdapter.notifyIsFirstFrameRendered(it)
                                }
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.playPosition }.distinctUntilChanged()
                        .collect {
                            //The Runnable has the potential to outlive the viewLifecycleScope,
                            // so we use an extra launch{} to make sure it only runs in the scope.
                            binding.recyclerView.post {
                                launch {
                                    mediaItemAdapter.notifyPlayPositionChanged(it)
                                }
                            }
                        }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mediaItemPagingDataFlow.collectLatest {
                mediaItemAdapter.submitData(lifecycle, it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerView.addOnScrollListener(onScrollListener)
    }

    override fun onPause() {
        super.onPause()
        binding.recyclerView.removeOnScrollListener(onScrollListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        _linearLayoutManger = null

        _mediaItemAdapter = null

        viewModel.setPlayPosition(null)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        viewModel.setPlayPosition(if (hidden) null else playPosition)
    }
}
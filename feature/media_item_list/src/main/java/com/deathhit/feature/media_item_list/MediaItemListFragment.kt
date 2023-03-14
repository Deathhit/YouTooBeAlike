package com.deathhit.feature.media_item_list

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.deathhit.core.ui.AppLoadStateAdapter
import com.deathhit.feature.media_item_list.model.MediaItemLabel
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.media_item_list.databinding.FragmentMediaItemListBinding
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MediaItemListFragment : Fragment() {
    companion object {
        fun create(mediaItemLabel: MediaItemLabel) =
            MediaItemListFragment().apply {
                arguments = MediaItemListViewModel.createArgs(mediaItemLabel)
            }
    }

    interface Callback {
        fun onOpenItem(itemId: String)
    }

    var callback: Callback? = null

    var player: Player? = null
        set(value) {
            if (field == value)
                return

            field?.apply {
                stop()
                removeListener(playerListener)
            }

            field = value?.apply { addListener(playerListener) }

            lifecycleScope.launchWhenCreated {
                viewModel.setIsPlayerSet(player != null)
            }
        }

    private val binding get() = _binding!!
    private var _binding: FragmentMediaItemListBinding? = null

    private val viewModel: MediaItemListViewModel by viewModels()

    private val linearLayoutManager get() = _linearLayoutManger!!
    private var _linearLayoutManger: LinearLayoutManager? = null
    private val firstCompletelyVisibleItemPosition
        get() = linearLayoutManager.findFirstCompletelyVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION)
                null
            else
                it
        }

    private val mediaItemAdapter get() = _mediaItemAdapter!!
    private var _mediaItemAdapter: MediaItemAdapter? = null

    private val onRefreshListener =
        SwipeRefreshLayout.OnRefreshListener { viewModel.refreshList() }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            viewModel.setFirstCompletelyVisibleItemPosition(firstCompletelyVisibleItemPosition)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (!isPlaying)
                with(player!!) {
                    viewModel.savePlayItemPosition(
                        playbackState == Player.STATE_ENDED,
                        currentPosition
                    )
                }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            viewModel.notifyFirstFrameRendered()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setIsViewInLandscape(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
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

            _mediaItemAdapter = object : MediaItemAdapter(Glide.with(view)) {
                override fun onBindPlayPosition(item: MediaItemVO) {
                    viewModel.prepareItem(item)
                }

                override fun onClickItem(item: MediaItemVO) {
                    viewModel.openItem(item)
                }
            }.also {
                adapter =
                    it.apply {
                        addOnPagesUpdatedListener {
                            if (itemCount > 0)
                                viewModel.scrollToTopOnFirstPageLoaded()
                        }

                        addLoadStateListener { loadStates ->
                            viewModel.setIsRefreshingList(loadStates.refresh is LoadState.Loading)
                        }
                    }.withLoadStateFooter(object : AppLoadStateAdapter() {
                        override fun onRetryLoading() {
                            viewModel.retryLoadingList()
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
                                        action.item.id
                                    )
                                    is MediaItemListViewModel.State.Action.PrepareAndPlayPlayback -> player!!.run {
                                        setMediaItem(
                                            MediaItem.fromUri(action.sourceUrl),
                                            if (action.isEnded) C.TIME_UNSET else action.position
                                        )
                                        prepare()
                                        play()
                                    }
                                    MediaItemListViewModel.State.Action.RefreshList -> mediaItemAdapter.refresh()
                                    MediaItemListViewModel.State.Action.RetryLoadingList -> mediaItemAdapter.retry()
                                    MediaItemListViewModel.State.Action.ScrollToTop -> binding.recyclerView.scrollToPosition(
                                        0
                                    )
                                    MediaItemListViewModel.State.Action.StopPlayback -> player!!.stop()
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.isRefreshingList }.distinctUntilChanged().collect {
                        binding.swipeRefreshLayout.isRefreshing = it
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.listState }.distinctUntilChanged().collect {
                        //The Runnable has the potential to outlive the viewLifecycleScope,
                        // so we use an extra launch{} to make sure it only runs within the scope.
                        binding.recyclerView.post {
                            launch {
                                mediaItemAdapter.notifyListStateChanged(
                                    it.isFirstFrameRendered,
                                    player,
                                    it.playPosition
                                )
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
        binding.apply {
            recyclerView.addOnScrollListener(onScrollListener)
            swipeRefreshLayout.setOnRefreshListener(onRefreshListener)
        }

        viewModel.setIsViewActive(true)
    }

    override fun onPause() {
        super.onPause()
        binding.apply {
            recyclerView.removeOnScrollListener(onScrollListener)
            swipeRefreshLayout.setOnRefreshListener(null)
        }

        viewModel.setIsViewActive(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Triggers RecyclerView.Adapter.onViewRecycled() to clear resources.
        binding.recyclerView.adapter = null

        _binding = null

        _linearLayoutManger = null

        _mediaItemAdapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null

        player = null

        viewModel.setIsPlayerSet(false) //Update state before onCreate() to ensure correctness.
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        lifecycleScope.launchWhenCreated {
            viewModel.setIsViewHidden(hidden)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }
}
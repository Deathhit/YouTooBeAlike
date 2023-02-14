package com.deathhit.feature.video_list.fragment.video_list

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
import com.deathhit.feature.video_list.adapter.video.VideoAdapter
import com.deathhit.feature.video_list.databinding.FragmentVideoListBinding
import com.deathhit.feature.video_list.model.VideoVO
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoListFragment : Fragment() {
    companion object {
        fun create() = VideoListFragment()
    }

    interface Callback {
        fun onClickItem(item: VideoVO)
    }

    var callback: Callback? = null

    var player: Player? = null
        set(value) {
            field?.removeListener(playerListener)
            field = value?.apply { addListener(playerListener) }
            _videoAdapter?.setPlayer(player)
        }

    private val binding get() = _binding!!
    private var _binding: FragmentVideoListBinding? = null

    private val viewModel: VideoListViewModel by viewModels()

    private val linearLayoutManager get() = _linearLayoutManger!!
    private var _linearLayoutManger: LinearLayoutManager? = null
    private val playPosition
        get() = linearLayoutManager.findFirstCompletelyVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION)
                null
            else
                it
        }

    private val videoAdapter get() = _videoAdapter!!
    private var _videoAdapter: VideoAdapter? = null

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            viewModel.setPlayPosition(playPosition)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (!isPlaying)
                player?.let {
                    viewModel.saveMediaPosition(
                        if (it.playbackState == Player.STATE_ENDED)
                            C.TIME_UNSET
                        else
                            it.currentPosition
                    )
                }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            viewModel.notifyFirstFrameRendered()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentVideoListBinding.inflate(inflater, container, false).run {
        _binding = this
        root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            setHasFixedSize(true)

            _linearLayoutManger = (layoutManager!! as LinearLayoutManager)

            _videoAdapter = object : VideoAdapter() {
                override fun onBindPlayPosition(item: VideoVO) {
                    viewModel.preparePlayItem(item)
                }

                override fun onClickItem(item: VideoVO) {
                    viewModel.clickItem(item)
                }
            }.apply { setPlayer(player) }.also {
                adapter = it.withLoadStateFooter(object : LoadStateAdapter() {
                    override fun onRetryLoading() {
                        videoAdapter.retry()
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
                                    is VideoListViewModel.State.Action.ClickItem -> callback?.onClickItem(
                                        action.item
                                    )
                                    is VideoListViewModel.State.Action.PrepareMedia -> player?.apply {
                                        setMediaItem(
                                            MediaItem.fromUri(action.item.sourceUrl),
                                            action.position
                                        )
                                        prepare()

                                        if (playbackState == Player.STATE_ENDED)
                                            seekToDefaultPosition()
                                    }
                                    VideoListViewModel.State.Action.StopMedia -> player?.stop()
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
                                    videoAdapter.notifyIsFirstFrameRendered(it)
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
                                    videoAdapter.notifyPlayPositionChanged(it)
                                }
                            }
                        }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.videoPagingDataFlow.collectLatest {
                videoAdapter.submitData(lifecycle, it)
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
        player = null

        _binding = null

        _linearLayoutManger = null

        _videoAdapter = null

        viewModel.setPlayPosition(null)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        viewModel.setPlayPosition(if (hidden) null else playPosition)
    }
}
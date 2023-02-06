package com.deathhit.feature.video_list.fragment.video_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.video_list.databinding.FragmentVideoListBinding
import com.deathhit.feature.video_list.model.MediaItemVO
import com.google.android.exoplayer2.ExoPlayer
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

    private val binding get() = _binding!!
    private var _binding: FragmentVideoListBinding? = null

    private val viewModel: VideoListViewModel by viewModels()

    private val linearLayoutManager get() = _linearLayoutManger!!
    private var _linearLayoutManger: LinearLayoutManager? = null

    private val player get() = _player!!
    private var _player: Player? = null

    private val videoAdapter get() = _videoAdapter!!
    private var _videoAdapter: VideoAdapter? = null

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            getPlayPosition()?.let {
                viewModel.prepareNewMedia(
                    player.currentPosition,
                    videoAdapter.peek(it)
                )
            }
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

            _videoAdapter =
                object : VideoAdapter(ExoPlayer.Builder(context).build().also { _player = it }) {
                    override fun onClickItem(item: MediaItemVO) {
                        viewModel.showItemClicked(item)
                    }
                }.also { adapter = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.actions }.distinctUntilChanged()
                        .collect { actions ->
                            actions.forEach { action ->
                                when (action) {
                                    is VideoListViewModel.State.Action.PrepareMedia -> with(player) {
                                        setMediaItem(
                                            MediaItem.fromUri(action.item.sourceUrl),
                                            action.position
                                        )
                                        prepare()
                                    }
                                    is VideoListViewModel.State.Action.ShowItemClicked -> Toast.makeText(
                                        requireContext(),
                                        getString(
                                            com.deathhit.core.ui.R.string.common_video_x_clicked,
                                            action.item.title
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    VideoListViewModel.State.Action.StopMedia -> player.stop()
                                }

                                viewModel.onAction(action)
                            }
                        }
                }

                launch {
                    viewModel.stateFlow.map { it.currentPlayingMedia }.distinctUntilChanged()
                        .collect {
                            binding.recyclerView.post {
                                videoAdapter.notifyCurrentPlayingItemChanged(it)
                            }
                        }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mediaItemPagingDataFlow.collectLatest {
                videoAdapter.submitData(lifecycle, it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            recyclerView.addOnScrollListener(onScrollListener)
        }

        player.play()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            recyclerView.removeOnScrollListener(onScrollListener)
        }

        player.pause()

        viewModel.saveCurrentMediaProgress(player.currentPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        _linearLayoutManger = null

        player.release()
        _player = null

        _videoAdapter = null
    }

    private fun getPlayPosition() =
        linearLayoutManager.findFirstCompletelyVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION)
                null
            else
                it
        }
}
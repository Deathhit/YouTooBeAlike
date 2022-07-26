package com.deathhit.video_list_example.fragment.video_list

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
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.deathhit.video_list_example.R
import com.deathhit.video_list_example.databinding.FragmentVideoListBinding
import com.deathhit.video_list_example.model.VideoVO
import dagger.hilt.android.AndroidEntryPoint
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

    private val videoAdapter get() = _videoAdapter!!
    private var _videoAdapter: VideoAdapter? = null

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            viewModel.onScrolled(getPlayPosition())
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
                object : VideoAdapter(requireContext()) {
                    override fun getItemPosition(playPosition: Int): Long =
                        viewModel.stateFlow.value.itemPositionMap.getOrElse(playPosition) { 0L }

                    override fun onClickItem(item: VideoVO) {
                        viewModel.onClickItem(item)
                    }

                    override fun onPlaybackEnded() {
                        viewModel.onPlaybackEnded()
                    }

                    override fun onSaveItemPosition(itemPosition: Long, playPosition: Int) {
                        viewModel.onSaveItemPosition(itemPosition, playPosition)
                    }
                }.also { adapter = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            VideoListViewModel.Event.ScrollToNextItem -> linearLayoutManager.startSmoothScroll(
                                object :
                                    LinearSmoothScroller(requireContext()) {
                                    override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                                }.apply {
                                    getPlayPosition()?.let {
                                        targetPosition = it + 1
                                    }
                                })
                            is VideoListViewModel.Event.ShowItemClicked -> Toast.makeText(
                                requireContext(),
                                getString(R.string.video_list_video_x_clicked, event.item.title),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.itemList }.distinctUntilChanged().collect {
                        videoAdapter.submitList(it)
                    }
                }

                launch {
                    viewModel.stateFlow.map { it.playPosition }.distinctUntilChanged().collect {
                        videoAdapter.notifyPlayPositionChanged(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            recyclerView.addOnScrollListener(onScrollListener)
        }

        videoAdapter.playVideo()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            recyclerView.removeOnScrollListener(onScrollListener)
        }

        videoAdapter.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        _linearLayoutManger = null

        _videoAdapter = with(videoAdapter) {
            release()
            null
        }
    }

    private fun getPlayPosition() =
        linearLayoutManager.findFirstCompletelyVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION)
                null
            else
                it
        }
}
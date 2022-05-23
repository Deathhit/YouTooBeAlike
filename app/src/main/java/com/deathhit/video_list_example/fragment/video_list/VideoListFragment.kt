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
            viewModel.setPlayPosition(getPlayPosition())
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
                    override fun getVideoPosition(itemPosition: Int): Long =
                        viewModel.stateFlow.value.argVideoPositionMap.getOrElse(itemPosition) { 0L }

                    override fun onClickItem(item: VideoVO) {
                        viewModel.onClickItem(item)
                    }

                    override fun onPlaybackEnded() {
                        viewModel.scrollToNextItem()
                    }

                    override fun onSaveVideoPosition(itemPosition: Int, videoPosition: Long) {
                        viewModel.saveVideoPosition(itemPosition, videoPosition)
                    }
                }.also { adapter = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateFlow.collect { state ->
                    with(state) {
                        eventOnClickItem.sign(viewModel) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.video_list_video_x_clicked, it.title),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        eventPlayAtPosition.sign(viewModel) {
                            videoAdapter.notifyPlayPositionChanged(it)
                        }

                        eventScrollToNextItem.sign(viewModel) {
                            linearLayoutManager.startSmoothScroll(object :
                                LinearSmoothScroller(requireContext()) {
                                override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                            }.apply { targetPosition = getPlayPosition() + 1 })
                        }

                        statusVideoList.sign(binding) {
                            videoAdapter.submitList(it)
                        }
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

    private fun getPlayPosition() = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
}
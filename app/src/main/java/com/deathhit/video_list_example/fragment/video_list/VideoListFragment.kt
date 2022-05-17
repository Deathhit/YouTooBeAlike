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
            viewModel.setPlayPos(getPlayPos())
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

            _videoAdapter = object : VideoAdapter(requireContext()) {
                override fun getVideoPosition(sourceUrl: String): Long =
                    viewModel.stateFlow.value.argVideoPositionMap.getOrElse(sourceUrl) { 0L }

                override fun onClickItem(item: VideoVO) {
                    //todo test
                    Toast.makeText(requireContext(), "FOO", Toast.LENGTH_LONG).show()
                }

                override fun onPlaybackEnded() {
                    linearLayoutManager.startSmoothScroll(object :
                        LinearSmoothScroller(requireContext()) {
                        override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                    }.apply { targetPosition = getPlayPos() + 1 })
                }

                override fun onSaveVideoPosition(sourceUrl: String, videoPosition: Long) {
                    viewModel.saveVideoPosition(sourceUrl, videoPosition)
                }
            }.also { adapter = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateFlow.collect { state ->
                    with(state) {
                        eventPlayAtPos.sign(viewModel) {
                            videoAdapter.saveVideoPosition()
                            videoAdapter.notifyPlayPosChanged(it)
                        }

                        eventOnClickVideo.sign(viewModel) {
                            //todo implement
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

        viewModel.setPlayPos(getPlayPos())
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
            saveVideoPosition()
            release()
            null
        }
    }

    private fun getPlayPos() = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
}
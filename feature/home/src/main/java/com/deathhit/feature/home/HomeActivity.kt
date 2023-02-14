package com.deathhit.feature.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.deathhit.feature.home.databinding.ActivityHomeBinding
import com.deathhit.feature.video_list.fragment.video_list.VideoListFragment
import com.deathhit.feature.video_list.model.VideoVO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "HomeActivity"
        private const val TAG_VIDEO_LIST = "$TAG.TAG_VIDEO_LIST"
    }

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeActivityViewModel by viewModels()

    private lateinit var player: Player

    private val videoListFragment
        get() = supportFragmentManager.findFragmentByTag(TAG_VIDEO_LIST) as VideoListFragment?

    private val onNavigationSelectedListener = OnItemSelectedListener {
        viewModel.setTab(
            when (it.itemId) {
                R.id.dashboard -> HomeActivityViewModel.State.Tab.DASHBOARD
                R.id.notifications -> HomeActivityViewModel.State.Tab.NOTIFICATIONS
                R.id.home -> HomeActivityViewModel.State.Tab.HOME
                else -> throw java.lang.RuntimeException("Unexpected item id of ${it.itemId}!")
            }
        )
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        player = ExoPlayer.Builder(this).build()

        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            when (fragment) {
                is VideoListFragment -> {
                    fragment.callback = object : VideoListFragment.Callback {
                        override fun onClickItem(item: VideoVO) {
                            viewModel.playVideo(item)
                        }
                    }

                    fragment.player = player
                }
            }
        }

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater).also { setContentView(it.root) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stateFlow.map { it.tab }.distinctUntilChanged().collect { tab ->
                        when (tab) {
                            HomeActivityViewModel.State.Tab.DASHBOARD -> {
                                supportFragmentManager.commit {
                                    videoListFragment?.let { hide(it) }
                                }

                                binding.bottomNavigationView.selectedItemId = R.id.dashboard
                            }
                            HomeActivityViewModel.State.Tab.HOME -> {
                                supportFragmentManager.commit {
                                    videoListFragment?.let { show(it) } ?: add(
                                        binding.containerHomeTab.id,
                                        VideoListFragment.create(),
                                        TAG_VIDEO_LIST
                                    )
                                }

                                binding.bottomNavigationView.selectedItemId = R.id.home
                            }
                            HomeActivityViewModel.State.Tab.NOTIFICATIONS -> {
                                supportFragmentManager.commit {
                                    videoListFragment?.let { hide(it) }
                                }

                                binding.bottomNavigationView.selectedItemId = R.id.notifications
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(onNavigationSelectedListener)
        }


        player.play()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            bottomNavigationView.setOnItemSelectedListener(null)
        }

        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }
}
package com.deathhit.feature.video_list.activity.video_list

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.commit
import com.deathhit.feature.video_list.databinding.ActivityVideoListBinding
import com.deathhit.feature.video_list.fragment.video_list.VideoListFragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VideoListActivity"
        private const val TAG_VIDEO_LIST = "$TAG.TAG_VIDEO_LIST"
    }

    private lateinit var binding: ActivityVideoListBinding

    private lateinit var player: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        player = ExoPlayer.Builder(this).build()

        supportFragmentManager.addFragmentOnAttachListener{ _, fragment ->
            when(fragment) {
                is VideoListFragment -> {
                    fragment.player = player
                }
            }
        }

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityVideoListBinding.inflate(layoutInflater).apply { setContentView(root) }

        savedInstanceState ?: supportFragmentManager.commit {
            add(binding.activityContainer.id, VideoListFragment.create(), TAG_VIDEO_LIST)
        }
    }

    override fun onResume() {
        super.onResume()
        player.play()
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
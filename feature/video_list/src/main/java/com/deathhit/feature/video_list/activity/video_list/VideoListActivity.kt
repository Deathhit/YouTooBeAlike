package com.deathhit.feature.video_list.activity.video_list

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.commit
import com.deathhit.feature.video_list.databinding.ActivityVideoListBinding
import com.deathhit.feature.video_list.fragment.video_list.VideoListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VideoListActivity"
        private const val TAG_VIDEO_LIST = "$TAG.TAG_VIDEO_LIST"
    }

    private lateinit var binding: ActivityVideoListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoListBinding.inflate(layoutInflater).apply { setContentView(root) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        savedInstanceState ?: supportFragmentManager.commit {
            add(binding.activityContainer.id, VideoListFragment.create(), TAG_VIDEO_LIST)
        }
    }
}
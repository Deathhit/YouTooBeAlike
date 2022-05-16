package com.deathhit.video_list_example.activity.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.deathhit.video_list_example.databinding.ActivityMainBinding
import com.deathhit.video_list_example.fragment.video_list.VideoListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_VIDEO_LIST = "$TAG.TAG_VIDEO_LIST"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply { setContentView(root) }

        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .add(binding.activityContainer.id, VideoListFragment.create(), TAG_VIDEO_LIST).commit()
    }
}
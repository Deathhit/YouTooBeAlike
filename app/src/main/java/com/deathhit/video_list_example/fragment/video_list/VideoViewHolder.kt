package com.deathhit.video_list_example.fragment.video_list

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.video_list_example.databinding.ItemVideoBinding
import com.deathhit.video_list_example.model.MediaItemVO

class VideoViewHolder(
    val binding: ItemVideoBinding,
    var item: MediaItemVO? = null
) : RecyclerView.ViewHolder(binding.root)
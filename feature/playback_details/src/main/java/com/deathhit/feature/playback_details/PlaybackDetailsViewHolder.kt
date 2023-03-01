package com.deathhit.feature.playback_details

import androidx.recyclerview.widget.RecyclerView
import com.deathhit.feature.playback_details.databinding.ItemPlaybackDetailsBinding
import com.deathhit.feature.playback_details.model.PlaybackDetailsVO

class PlaybackDetailsViewHolder(
    val binding: ItemPlaybackDetailsBinding,
    var item: PlaybackDetailsVO? = null
) : RecyclerView.ViewHolder(binding.root)
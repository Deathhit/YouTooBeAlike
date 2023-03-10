package com.deathhit.feature.playback_details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.deathhit.feature.playback_details.databinding.ItemPlaybackDetailsBinding
import com.deathhit.feature.playback_details.model.PlaybackDetailsVO

class PlaybackDetailsAdapter : ListAdapter<PlaybackDetailsVO, PlaybackDetailsViewHolder>(COMPARATOR) {
    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<PlaybackDetailsVO>() {
            override fun areItemsTheSame(oldItem: PlaybackDetailsVO, newItem: PlaybackDetailsVO): Boolean =
                oldItem.mediaItemId == newItem.mediaItemId

            override fun areContentsTheSame(oldItem: PlaybackDetailsVO, newItem: PlaybackDetailsVO): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaybackDetailsViewHolder =
        PlaybackDetailsViewHolder(
            ItemPlaybackDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: PlaybackDetailsViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            holder.binding.textViewDescription.apply {
                text = item.description
            }

            holder.binding.textViewSubtitle.apply {
                text = item.subtitle
            }

            holder.binding.textViewTitle.apply {
                text = item.title
            }
        }
    }
}
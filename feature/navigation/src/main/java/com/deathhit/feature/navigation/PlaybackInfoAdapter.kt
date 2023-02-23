package com.deathhit.feature.navigation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.navigation.databinding.ItemPlaybackInfoBinding

class PlaybackInfoAdapter : ListAdapter<MediaItemVO, PlaybackInfoViewHolder>(COMPARATOR) {
    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<MediaItemVO>() {
            override fun areItemsTheSame(oldItem: MediaItemVO, newItem: MediaItemVO): Boolean =
                oldItem.sourceUrl == newItem.sourceUrl

            override fun areContentsTheSame(oldItem: MediaItemVO, newItem: MediaItemVO): Boolean =
                oldItem == newItem
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaybackInfoViewHolder = PlaybackInfoViewHolder(
        ItemPlaybackInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PlaybackInfoViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            with(holder.binding.textViewDescription) {
                text = item.description
            }

            with(holder.binding.textViewSubtitle) {
                text = item.subtitle
            }

            with(holder.binding.textViewTitle) {
                text = item.title
            }
        }
    }
}
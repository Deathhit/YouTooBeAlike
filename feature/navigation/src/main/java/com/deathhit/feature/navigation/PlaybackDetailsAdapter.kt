package com.deathhit.feature.navigation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.deathhit.feature.navigation.databinding.ItemPlaybackDetailsBinding
import com.deathhit.feature.navigation.model.PlaybackDetailsVO

class PlaybackDetailsAdapter : ListAdapter<PlaybackDetailsVO, PlaybackDetailsViewHolder>(COMPARATOR) {
    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<PlaybackDetailsVO>() {
            override fun areItemsTheSame(oldItem: PlaybackDetailsVO, newItem: PlaybackDetailsVO): Boolean =
                oldItem == newItem

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
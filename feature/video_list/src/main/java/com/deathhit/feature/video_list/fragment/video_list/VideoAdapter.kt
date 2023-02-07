package com.deathhit.feature.video_list.fragment.video_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.deathhit.feature.video_list.databinding.ItemVideoBinding
import com.deathhit.feature.video_list.model.VideoVO
import com.google.android.exoplayer2.Player

abstract class VideoAdapter(private val player: Player) :
    PagingDataAdapter<VideoVO, VideoViewHolder>(COMPARATOR) {
    companion object {
        private const val TAG = "VideoAdapter"
        private const val PAYLOAD_PLAY_POSITION = "$TAG.PAYLOAD_PLAY_POSITION"

        private val COMPARATOR = object : DiffUtil.ItemCallback<VideoVO>() {
            override fun areItemsTheSame(oldItem: VideoVO, newItem: VideoVO): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: VideoVO, newItem: VideoVO): Boolean =
                oldItem == newItem
        }
    }

    private var currentPlayingItem: VideoVO? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (player.playbackState == Player.STATE_READY)
                    notifyCurrentPlayingItemChanged(currentPlayingItem)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder =
        VideoViewHolder(
            ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ).apply {
            itemView.setOnClickListener { item?.let { onClickItem(it) } }
        }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            with(holder.binding.imageViewThumbnail) {
                Glide.with(this).load(item.thumbUrl).placeholder(com.deathhit.core.ui.R.color.black).into(this)
            }

            with(holder.binding.textViewSubtitle) {
                text = item.subtitle
            }

            with(holder.binding.textViewTitle) {
                text = item.title
            }

            bindPlayPosition(holder, item)
        }
    }

    override fun onBindViewHolder(
        holder: VideoViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else {
            holder.item = getItem(position)?.also { item ->
                payloads.forEach { payload ->
                    when (payload) {
                        PAYLOAD_PLAY_POSITION -> bindPlayPosition(holder, item)
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        with(holder.binding.imageViewThumbnail) {
            Glide.with(this).clear(this)
        }
    }

    fun notifyCurrentPlayingItemChanged(newPlayingItem: VideoVO?) {
        val items = snapshot().items
        val currentPlayPos = items.indexOf(currentPlayingItem)
        val newPlayPos = items.indexOf(newPlayingItem)

        currentPlayingItem = newPlayingItem
        notifyItemChanged(currentPlayPos, PAYLOAD_PLAY_POSITION)
        notifyItemChanged(newPlayPos, PAYLOAD_PLAY_POSITION)
    }

    private fun bindPlayPosition(holder: VideoViewHolder, item: VideoVO) {
        val isItemPlaying = isItemPlaying(item)
        val isPlayerViewVisible = isItemPlaying && player.playbackState == Player.STATE_READY
        val player = if (isItemPlaying) this@VideoAdapter.player else null

        with(holder.binding.imageViewThumbnail) {
            visibility = if (isPlayerViewVisible)
                View.INVISIBLE
            else
                View.VISIBLE
        }

        with(holder.binding.styledPlayerControllerView) {
            this.player = player
            if (isPlayerViewVisible)
                show()
            else
                hide()
        }

        with(holder.binding.styledPlayerView) {
            this.player = player
        }
    }

    private fun isItemPlaying(item: VideoVO) = currentPlayingItem == item

    abstract fun onClickItem(item: VideoVO)
}
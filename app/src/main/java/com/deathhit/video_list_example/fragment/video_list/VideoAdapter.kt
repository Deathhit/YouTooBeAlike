package com.deathhit.video_list_example.fragment.video_list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.deathhit.video_list_example.databinding.ItemVideoBinding
import com.deathhit.video_list_example.model.VideoVO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

//todo implement
abstract class VideoAdapter(context: Context) : ListAdapter<VideoVO, VideoViewHolder>(COMPARATOR) {
    companion object {
        private const val TAG = "VideoViewAdapter"
        private const val PAYLOAD_PLAY_POS_CHANGED = "$TAG.PAYLOAD_PLAY_POS_CHANGED"

        private val COMPARATOR = object : DiffUtil.ItemCallback<VideoVO>() {
            override fun areItemsTheSame(oldItem: VideoVO, newItem: VideoVO): Boolean =
                oldItem.sourceUrl == newItem.sourceUrl

            override fun areContentsTheSame(oldItem: VideoVO, newItem: VideoVO): Boolean =
                oldItem == newItem
        }
    }

    private val mediaItemMap: MutableMap<String, MediaItem> = mutableMapOf()

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    seekTo(0L)
                    onPlaybackEnded()
                }
            }
        })
    }

    private var playPos: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder =
        VideoViewHolder(
            ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ).apply {
            itemView.setOnClickListener { item?.let { onClickItem(it) } }
            binding.styledPlayerView.setOnClickListener { itemView.performClick() }
        }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.item = getItem(position)?.also { item ->
            with(holder.binding.imageViewThumbnail) {
                Glide.with(this).load(item.thumbUrl).into(this)
            }

            bindPlayPos(holder, item, position)
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
                        PAYLOAD_PLAY_POS_CHANGED -> bindPlayPos(holder, item, position)
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

    fun notifyPlayPosChanged(playPos: Int) {
        if (this.playPos != playPos) {
            player.stop()
            this.playPos = playPos
            notifyItemRangeChanged(0, itemCount, PAYLOAD_PLAY_POS_CHANGED)
        }
    }

    fun pauseVideo() {
        player.pause()
    }

    fun playVideo() {
        player.play()
    }

    fun release() {
        player.release()
    }

    fun saveVideoPosition() {
        if (playPos in 0 until itemCount)
            getItem(playPos).run {
                onSaveVideoPosition(sourceUrl, player.currentPosition)
            }
    }

    private fun bindPlayPos(holder: VideoViewHolder, item: VideoVO, position: Int) {
        val isAtPlayPos = position == playPos
        with(holder.binding.imageViewThumbnail) {
            visibility = if (isAtPlayPos) View.INVISIBLE else View.VISIBLE
        }

        with(holder.binding.styledPlayerView) {
            visibility = if (isAtPlayPos) View.VISIBLE else View.INVISIBLE
        }

        var player: ExoPlayer? = null
        if (isAtPlayPos)
            player = this.player.apply {
                if (playbackState == Player.STATE_IDLE) {
                    val sourceUrl = item.sourceUrl
                    setMediaItem(mediaItemMap.getOrPut(sourceUrl) {
                        MediaItem.fromUri(sourceUrl)
                    })
                    seekTo(getVideoPosition(sourceUrl))
                    prepare()
                }
            }

        with(holder.binding.styledPlayerView) {
            this.player = player
            if (isAtPlayPos)
                showController()
        }
    }

    abstract fun getVideoPosition(sourceUrl: String): Long
    abstract fun onClickItem(item: VideoVO)
    abstract fun onPlaybackEnded()
    abstract fun onSaveVideoPosition(sourceUrl: String, videoPosition: Long)
}
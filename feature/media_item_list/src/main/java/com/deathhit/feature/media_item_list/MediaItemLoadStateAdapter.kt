package com.deathhit.feature.media_item_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import com.deathhit.feature.media_item_list.databinding.ItemMediaItemLoadStateBinding

abstract class MediaItemLoadStateAdapter : androidx.paging.LoadStateAdapter<MediaItemLoadStateViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): MediaItemLoadStateViewHolder =
        MediaItemLoadStateViewHolder(
            ItemMediaItemLoadStateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ).apply {
            binding.run {
                buttonRetry.setOnClickListener { onRetryLoading() }
            }
        }

    override fun onBindViewHolder(holder: MediaItemLoadStateViewHolder, loadState: LoadState) {
        with(holder.binding.buttonRetry) {
            visibility = toVisibility(loadState !is LoadState.Loading)
        }

        with(holder.binding.progressBar) {
            visibility = toVisibility(loadState is LoadState.Loading)
        }

        with(holder.binding.textViewErrorMsg) {
            if (loadState is LoadState.Error)
                text = loadState.error.localizedMessage
            visibility = toVisibility(loadState !is LoadState.Loading)
        }
    }

    private fun toVisibility(constraint: Boolean): Int = if (constraint)
        View.VISIBLE
    else
        View.GONE

    abstract fun onRetryLoading()
}
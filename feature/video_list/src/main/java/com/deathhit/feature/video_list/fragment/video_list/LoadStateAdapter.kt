package com.deathhit.feature.video_list.fragment.video_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import com.deathhit.feature.video_list.databinding.ItemVideoListLoadStateBinding

abstract class LoadStateAdapter :
    LoadStateAdapter<LoadStateViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder =
        LoadStateViewHolder(
            ItemVideoListLoadStateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ).apply {
            binding.run {
                buttonRetry.setOnClickListener { onRetryLoading() }
            }
        }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
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
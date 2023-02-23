package com.deathhit.core.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import com.deathhit.core.ui.databinding.ItemAppLoadStateBinding

abstract class AppLoadStateAdapter : androidx.paging.LoadStateAdapter<AppLoadStateViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): AppLoadStateViewHolder =
        AppLoadStateViewHolder(
            ItemAppLoadStateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ).apply {
            binding.run {
                buttonRetry.setOnClickListener { onRetryLoading() }
            }
        }

    override fun onBindViewHolder(holder: AppLoadStateViewHolder, loadState: LoadState) {
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
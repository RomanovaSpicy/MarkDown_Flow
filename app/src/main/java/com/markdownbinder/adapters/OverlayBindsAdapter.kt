package com.markdownbinder.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.markdownbinder.databinding.ItemOverlayBindBinding
import com.markdownbinder.models.Bind

class OverlayBindsAdapter(
    private val onBindClick: (Bind) -> Unit
) : ListAdapter<Bind, OverlayBindsAdapter.BindViewHolder>(BindDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindViewHolder {
        val binding = ItemOverlayBindBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BindViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BindViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BindViewHolder(
        private val binding: ItemOverlayBindBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bind: Bind) {
            binding.overlayBindName.text = bind.name
            binding.overlayBindContent.text = bind.content
            
            binding.root.setOnClickListener {
                onBindClick(bind)
            }
        }
    }

    private class BindDiffCallback : DiffUtil.ItemCallback<Bind>() {
        override fun areItemsTheSame(oldItem: Bind, newItem: Bind): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Bind, newItem: Bind): Boolean {
            return oldItem == newItem
        }
    }
}

package com.markdownbinder.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.markdownbinder.MainActivity
import com.markdownbinder.R
import com.markdownbinder.databinding.ItemBindBinding
import com.markdownbinder.models.Bind

class BindsAdapter(
    private val onBindClick: (Bind) -> Unit,
    private val onBindEdit: (Bind) -> Unit,
    private val onBindDelete: (Bind) -> Unit
) : ListAdapter<Bind, BindsAdapter.BindViewHolder>(BindDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindViewHolder {
        val binding = ItemBindBinding.inflate(
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
        private val binding: ItemBindBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(bind: Bind) {
            binding.bindName.text = bind.name
            binding.bindContent.text = bind.content

            binding.bindCard.setOnClickListener { onBindClick(bind) }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    (itemView.context as? MainActivity)?.getItemTouchHelper()?.startDrag(this)
                }
                false
            }

            binding.moreButton.setOnClickListener { showActionMenu(bind) }
        }

        private fun showActionMenu(bind: Bind) {
            val context = binding.root.context
            val items = arrayOf(
                context.getString(R.string.edit),
                context.getString(R.string.delete)
            )

            MaterialAlertDialogBuilder(context)
                .setTitle(bind.name)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> onBindEdit(bind)
                        1 -> onBindDelete(bind)
                    }
                }
                .show()
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
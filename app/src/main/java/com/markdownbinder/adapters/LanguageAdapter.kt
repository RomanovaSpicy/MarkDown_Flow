package com.markdownbinder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.markdownbinder.databinding.ItemLanguageCardBinding

class LanguageAdapter(
    private val onLanguageSelected: (Language) -> Unit
) : ListAdapter<Language, LanguageAdapter.LanguageViewHolder>(LanguageDiffCallback()) {

    private var selectedLanguageCode: String = "en"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setSelectedLanguage(languageCode: String) {
        val oldSelectedPos = currentList.indexOfFirst { it.code == selectedLanguageCode }
        val newSelectedPos = currentList.indexOfFirst { it.code == languageCode }
        
        selectedLanguageCode = languageCode
        
        if (oldSelectedPos != -1) notifyItemChanged(oldSelectedPos)
        if (newSelectedPos != -1) notifyItemChanged(newSelectedPos)
    }

    inner class LanguageViewHolder(
        private val binding: ItemLanguageCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(language: Language) {
            binding.languageFlag.text = language.flag
            binding.languageName.text = binding.root.context.getString(language.nameRes)

            val isSelected = language.code == selectedLanguageCode
            binding.selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            binding.languageCard.strokeWidth = if (isSelected) 2 else 1
            
            binding.languageCard.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }

    private class LanguageDiffCallback : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem == newItem
        }
    }
}


data class Language(
    val code: String,        // ISO (en, ru, fr, etc.)
    val flag: String,        // Emoji
    val nameRes: Int         // String resource ID
)

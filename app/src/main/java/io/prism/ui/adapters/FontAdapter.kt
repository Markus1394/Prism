package io.prism.ui.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.prism.R
import io.prism.data.model.FontResource

class FontAdapter(
    private val onFontSelected: (FontResource) -> Unit
) : ListAdapter<FontResource, FontAdapter.FontViewHolder>(FontDiffCallback()) {

    private var selectedId: String? = null

    fun setSelectedFont(font: FontResource) {
        val oldSelectedId = selectedId
        selectedId = font.id

        currentList.forEachIndexed { index, item ->
            if (item.id == oldSelectedId || item.id == selectedId) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_font, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fontNameTextView: TextView = itemView.findViewById(R.id.fontNameText)
        private val selectionIndicator: View = itemView.findViewById(R.id.selectionIndicator)

        fun bind(font: FontResource, selectedId: String?) {
            fontNameTextView.text = itemView.context.getString(font.nameResId)

            
            val typeface = try {
                ResourcesCompat.getFont(itemView.context, font.fontResId)
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
            fontNameTextView.typeface = typeface

            val isSelected = font.id == selectedId
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                onFontSelected(font)
            }
        }
    }

    private class FontDiffCallback : DiffUtil.ItemCallback<FontResource>() {
        override fun areItemsTheSame(oldItem: FontResource, newItem: FontResource) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FontResource, newItem: FontResource) =
            oldItem == newItem
    }
}
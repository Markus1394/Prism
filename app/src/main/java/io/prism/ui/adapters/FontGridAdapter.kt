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

class FontGridAdapter(
    private val onFontSelected: (FontResource) -> Unit
) : ListAdapter<FontResource, FontGridAdapter.FontViewHolder>(FontDiffCallback()) {

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
            .inflate(R.layout.item_font_grid, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fontName: TextView = itemView.findViewById(R.id.fontNameText)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)

        fun bind(font: FontResource, selectedId: String?) {
            val context = itemView.context

            fontName.text = context.getString(font.nameResId)

            
            val typeface = if (font.fontResId != 0) {
                try {
                    ResourcesCompat.getFont(context, font.fontResId)
                } catch (e: Exception) {
                    Typeface.DEFAULT
                }
            } else {
                Typeface.DEFAULT
            }
            fontName.typeface = typeface

            val isSelected = font.id == selectedId
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

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
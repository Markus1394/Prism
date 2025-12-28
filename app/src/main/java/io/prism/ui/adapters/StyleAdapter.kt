package io.prism.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.prism.R
import io.prism.data.model.WatermarkStyle

class StyleAdapter(
    private val onStyleSelected: (WatermarkStyle) -> Unit
) : ListAdapter<WatermarkStyle, StyleAdapter.StyleViewHolder>(StyleDiffCallback()) {

    private var selectedId: String? = null

    fun setSelectedStyle(style: WatermarkStyle) {
        val oldSelectedId = selectedId
        selectedId = style.id

        currentList.forEachIndexed { index, item ->
            if (item.id == oldSelectedId || item.id == selectedId) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_style, parent, false)
        return StyleViewHolder(view)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class StyleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val styleNameTextView: TextView = itemView.findViewById(R.id.styleNameText)
        private val styleDescTextView: TextView = itemView.findViewById(R.id.styleDescText)
        private val selectionIndicator: View = itemView.findViewById(R.id.selectionIndicator)

        fun bind(style: WatermarkStyle, selectedId: String?) {
            styleNameTextView.text = itemView.context.getString(style.nameResId)

            style.descriptionResId?.let {
                styleDescTextView.text = itemView.context.getString(it)
                styleDescTextView.visibility = View.VISIBLE
            } ?: run {
                styleDescTextView.visibility = View.GONE
            }

            val isSelected = style.id == selectedId
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                onStyleSelected(style)
            }
        }
    }

    private class StyleDiffCallback : DiffUtil.ItemCallback<WatermarkStyle>() {
        override fun areItemsTheSame(oldItem: WatermarkStyle, newItem: WatermarkStyle) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WatermarkStyle, newItem: WatermarkStyle) =
            oldItem == newItem
    }
}
package io.prism.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.prism.R
import io.prism.data.model.WatermarkStyle

class StyleGridAdapter(
    private val onStyleSelected: (WatermarkStyle) -> Unit,
    private val onAddCustomTemplate: () -> Unit,
    private val onDeleteCustomTemplate: (WatermarkStyle) -> Unit
) : ListAdapter<StyleGridAdapter.StyleItem, RecyclerView.ViewHolder>(StyleDiffCallback()) {

    private var selectedId: String? = null

    sealed class StyleItem {
        data class Style(val resource: WatermarkStyle) : StyleItem()
        object AddButton : StyleItem()
    }

    fun setSelectedStyle(style: WatermarkStyle) {
        val oldSelectedId = selectedId
        selectedId = style.id

        currentList.forEachIndexed { index, item ->
            if (item is StyleItem.Style) {
                if (item.resource.id == oldSelectedId || item.resource.id == selectedId) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    fun submitStyles(styles: List<WatermarkStyle>) {
        val items = styles.map { StyleItem.Style(it) } + StyleItem.AddButton
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StyleItem.Style -> VIEW_TYPE_STYLE
            is StyleItem.AddButton -> VIEW_TYPE_ADD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STYLE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_style_grid, parent, false)
                StyleViewHolder(view)
            }
            VIEW_TYPE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_template, parent, false)
                AddTemplateViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is StyleItem.Style -> (holder as StyleViewHolder).bind(item.resource, selectedId)
            is StyleItem.AddButton -> (holder as AddTemplateViewHolder).bind()
        }
    }

    inner class StyleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val styleName: TextView = itemView.findViewById(R.id.styleNameText)
        private val styleDesc: TextView = itemView.findViewById(R.id.styleDescText)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(style: WatermarkStyle, selectedId: String?) {
            val context = itemView.context

            styleName.text = when {
                style.nameResId != 0 -> context.getString(style.nameResId)
                !style.customName.isNullOrBlank() -> style.customName
                else -> ""
            }

            val description = when {
                style.descriptionResId != null && style.descriptionResId != 0 ->
                    context.getString(style.descriptionResId)
                !style.customDescription.isNullOrBlank() -> style.customDescription
                else -> null
            }

            if (description != null) {
                styleDesc.text = description
                styleDesc.visibility = View.VISIBLE
            } else {
                styleDesc.visibility = View.GONE
            }

            
            deleteButton.visibility = if (style.isCustom) View.VISIBLE else View.GONE
            deleteButton.setOnClickListener {
                onDeleteCustomTemplate(style)
            }

            val isSelected = style.id == selectedId
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onStyleSelected(style)
            }
        }
    }

    inner class AddTemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.setOnClickListener {
                onAddCustomTemplate()
            }
        }
    }

    private class StyleDiffCallback : DiffUtil.ItemCallback<StyleItem>() {
        override fun areItemsTheSame(oldItem: StyleItem, newItem: StyleItem): Boolean {
            return when {
                oldItem is StyleItem.Style && newItem is StyleItem.Style ->
                    oldItem.resource.id == newItem.resource.id
                oldItem is StyleItem.AddButton && newItem is StyleItem.AddButton -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: StyleItem, newItem: StyleItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_STYLE = 0
        private const val VIEW_TYPE_ADD = 1
    }
}
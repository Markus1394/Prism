package io.prism.ui.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.prism.R
import io.prism.data.model.LogoResource
import java.io.File

class LogoGridAdapter(
    private val onLogoSelected: (LogoResource) -> Unit,
    private val onAddCustomLogo: () -> Unit,
    private val onDeleteCustomLogo: (LogoResource) -> Unit
) : ListAdapter<LogoGridAdapter.LogoItem, RecyclerView.ViewHolder>(LogoDiffCallback()) {

    private var selectedId: String? = null

    sealed class LogoItem {
        data class Logo(val resource: LogoResource) : LogoItem()
        object AddButton : LogoItem()
    }

    fun setSelectedLogo(logo: LogoResource?) {
        val oldSelectedId = selectedId
        selectedId = logo?.id

        currentList.forEachIndexed { index, item ->
            if (item is LogoItem.Logo) {
                if (item.resource.id == oldSelectedId || item.resource.id == selectedId) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    fun submitLogos(logos: List<LogoResource>) {
        val items = logos.map { LogoItem.Logo(it) } + LogoItem.AddButton
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LogoItem.Logo -> VIEW_TYPE_LOGO
            is LogoItem.AddButton -> VIEW_TYPE_ADD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOGO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_logo_grid, parent, false)
                LogoViewHolder(view)
            }
            VIEW_TYPE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_logo, parent, false)
                AddLogoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LogoItem.Logo -> (holder as LogoViewHolder).bind(item.resource, selectedId)
            is LogoItem.AddButton -> (holder as AddLogoViewHolder).bind()
        }
    }

    inner class LogoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImage: ImageView = itemView.findViewById(R.id.logoImage)
        private val logoName: TextView = itemView.findViewById(R.id.logoNameText)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)
        private val noLogoIcon: ImageView = itemView.findViewById(R.id.noLogoIcon)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(logo: LogoResource, selectedId: String?) {
            val context = itemView.context

            
            logoName.text = when {
                logo.nameResId != 0 -> context.getString(logo.nameResId)
                !logo.customName.isNullOrBlank() -> logo.customName
                else -> ""
            }

            
            deleteButton.visibility = if (logo.isCustom) View.VISIBLE else View.GONE
            deleteButton.setOnClickListener {
                onDeleteCustomLogo(logo)
            }

            when {
                logo.isNoLogo -> {
                    logoImage.visibility = View.GONE
                    noLogoIcon.visibility = View.VISIBLE
                }
                logo.isCustom && !logo.customLogoPath.isNullOrEmpty() -> {
                    logoImage.visibility = View.VISIBLE
                    noLogoIcon.visibility = View.GONE
                    val file = File(logo.customLogoPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        logoImage.setImageBitmap(bitmap)
                    }
                }
                logo.drawableResId != 0 -> {
                    logoImage.visibility = View.VISIBLE
                    noLogoIcon.visibility = View.GONE
                    logoImage.setImageResource(logo.drawableResId)
                }
                else -> {
                    logoImage.visibility = View.GONE
                    noLogoIcon.visibility = View.VISIBLE
                }
            }

            logoImage.colorFilter = null

            val isSelected = logo.id == selectedId
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onLogoSelected(logo)
            }
        }
    }

    inner class AddLogoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.setOnClickListener {
                onAddCustomLogo()
            }
        }
    }

    private class LogoDiffCallback : DiffUtil.ItemCallback<LogoItem>() {
        override fun areItemsTheSame(oldItem: LogoItem, newItem: LogoItem): Boolean {
            return when {
                oldItem is LogoItem.Logo && newItem is LogoItem.Logo ->
                    oldItem.resource.id == newItem.resource.id
                oldItem is LogoItem.AddButton && newItem is LogoItem.AddButton -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: LogoItem, newItem: LogoItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_LOGO = 0
        private const val VIEW_TYPE_ADD = 1
    }
}
package io.prism.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.prism.R
import io.prism.data.model.LogoResource

class LogoAdapter(
    private val onLogoSelected: (LogoResource) -> Unit
) : ListAdapter<LogoResource, LogoAdapter.LogoViewHolder>(LogoDiffCallback()) {

    private var selectedId: String? = null

    fun setSelectedLogo(logo: LogoResource) {
        val oldSelectedId = selectedId
        selectedId = logo.id

        currentList.forEachIndexed { index, item ->
            if (item.id == oldSelectedId || item.id == selectedId) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logo, parent, false)
        return LogoViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogoViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class LogoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImageView: ImageView = itemView.findViewById(R.id.logoImage)
        private val logoNameTextView: TextView = itemView.findViewById(R.id.logoNameText)
        private val selectionIndicator: View = itemView.findViewById(R.id.selectionIndicator)

        fun bind(logo: LogoResource, selectedId: String?) {
            logoImageView.setImageResource(logo.drawableResId)
            logoNameTextView.text = itemView.context.getString(logo.nameResId)

            val isSelected = logo.id == selectedId
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                onLogoSelected(logo)
            }
        }
    }

    private class LogoDiffCallback : DiffUtil.ItemCallback<LogoResource>() {
        override fun areItemsTheSame(oldItem: LogoResource, newItem: LogoResource) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogoResource, newItem: LogoResource) =
            oldItem == newItem
    }
}
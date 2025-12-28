package io.prism.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.prism.R
import io.prism.data.model.WatermarkPosition

class PositionAdapter(
    private val onPositionSelected: (WatermarkPosition) -> Unit
) : RecyclerView.Adapter<PositionAdapter.PositionViewHolder>() {

    private val positions = WatermarkPosition.values().toList()
    private var selectedPosition: WatermarkPosition = WatermarkPosition.BOTTOM

    fun setSelectedPosition(position: WatermarkPosition) {
        val oldPosition = selectedPosition
        selectedPosition = position

        val oldIndex = positions.indexOf(oldPosition)
        val newIndex = positions.indexOf(position)

        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun getItemCount(): Int = positions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position, parent, false)
        return PositionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        holder.bind(positions[position], selectedPosition)
    }

    inner class PositionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionIcon: ImageView = itemView.findViewById(R.id.positionIcon)
        private val positionName: TextView = itemView.findViewById(R.id.positionNameText)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)

        fun bind(position: WatermarkPosition, selectedPosition: WatermarkPosition) {
            val context = itemView.context

            val (nameRes, iconRes) = when (position) {
                WatermarkPosition.TOP -> R.string.position_top to R.drawable.ic_position_top
                WatermarkPosition.BOTTOM -> R.string.position_bottom to R.drawable.ic_position_bottom
                WatermarkPosition.LEFT -> R.string.position_left to R.drawable.ic_position_left
                WatermarkPosition.RIGHT -> R.string.position_right to R.drawable.ic_position_right
            }

            positionName.text = context.getString(nameRes)
            positionIcon.setImageResource(iconRes)

            val isSelected = position == selectedPosition
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onPositionSelected(position)
            }
        }
    }
}
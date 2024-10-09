package com.meetfriend.app.ui.camerakit.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.snap.camerakit.lenses.LensesComponent

internal class LensesAdapter(
    private val onItemClicked: (LensesComponent.Lens) -> Unit
) : ListAdapter<LensesComponent.Lens, LensesAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var selectedPosition = -1

    fun select(lens: LensesComponent.Lens) {
        val position = currentList.indexOf(lens)
        if (position != -1) {
            val previousSelectedPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(position)
        }
    }

    fun deselect() {
        val previousSelectedPosition = selectedPosition
        selectedPosition = -1
        notifyItemChanged(previousSelectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.lens_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
        holder.itemView.isSelected = selectedPosition == position
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        init {
            view.setOnClickListener(this)
        }

        private val lensNameView: TextView = view.findViewById(R.id.text_view_lens_name)
        private val lensIconView: ImageView = view.findViewById(R.id.image_view_lens_icon)

        fun bindTo(lens: LensesComponent.Lens) {
            lensNameView.text = lens.name
            Glide.with(itemView)
                .load(lens.icons.find { it is LensesComponent.Lens.Media.Image.Webp }?.uri)
                .into(lensIconView)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClicked(getItem(position))
            }
        }
    }

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LensesComponent.Lens>() {

            override fun areItemsTheSame(
                oldItem: LensesComponent.Lens,
                newItem: LensesComponent.Lens
            ) = oldItem.id == newItem.id

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: LensesComponent.Lens,
                newItem: LensesComponent.Lens
            ) = oldItem == newItem
        }
    }
}

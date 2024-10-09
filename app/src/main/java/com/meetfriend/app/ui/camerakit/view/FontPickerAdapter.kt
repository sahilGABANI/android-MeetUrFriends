package com.meetfriend.app.ui.camerakit.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.FontDetails

@SuppressLint("RecyclerView")
class FontPickerAdapter(private val context: Context, private val fontPickerList: List<FontDetails>) :
    RecyclerView.Adapter<FontPickerAdapter.ViewHolder>() {
    private var onFontPickerClickListener: OnFontPickerClickListener? = null
    private var selectedPosition = 0

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (fontPickerList[position].fontId != null) {
            val typeface: Typeface? = ResourcesCompat.getFont(context, fontPickerList[position].fontId!!)
            holder.fontPickerView.typeface = typeface
        }
        holder.fontPickerView.setText(fontPickerList[position].fontName)
        holder.rlMain.setBackgroundResource(
            if (position == selectedPosition) {
                R.drawable.font_selected_bg
            } else {
                R.drawable.font_unselected_bg
            }
        )

        holder.fontPickerView.setTextColor(
            if (position == selectedPosition) {
                context.resources.getColor(android.R.color.white,null)
            } else {
                context.resources.getColor(android.R.color.white,null)
            }
        )

        holder.fontPickerView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged() // Update the UI for the selected item
            fontPickerList[position].fontId?.let { it1 -> onFontPickerClickListener?.onFontPickerClickListener(it1) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.font_item_view, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return fontPickerList.size
    }



    fun setOnFontPickerClickListener(onFontPickerClickListener: OnFontPickerClickListener?) {
        this.onFontPickerClickListener = onFontPickerClickListener
    }


     class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var fontPickerView: TextView = itemView.findViewById(R.id.font_picker_view)
        var rlMain: RelativeLayout = itemView.findViewById(R.id.rl_Main)
    }


    interface OnFontPickerClickListener {
        fun onFontPickerClickListener(fontCode: Int)
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }


}
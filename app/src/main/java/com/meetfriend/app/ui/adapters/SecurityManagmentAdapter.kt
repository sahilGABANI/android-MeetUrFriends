package com.meetfriend.app.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.R
import com.meetfriend.app.responseclasses.settings.data

class SecurityManagmentAdapter(
    var callBack: AdapterCallback,
    val context: Context
) : RecyclerView.Adapter<SecurityManagmentAdapter.ViewHolder>() {
    interface AdapterCallback {
        fun itemClick(result: data)
    }
    private var securityList: List<data> = listOf()

    fun submitList(list: List<data>) {
        securityList = list
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val v =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.itemview_securitymanagment_item, parent, false)
        return ViewHolder(
            v
        )
    }

    override fun getItemCount(): Int {
        return securityList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataItem = securityList[position]
        val isLastItem = position == securityList.size - 1
        holder.bindItems(dataItem, callBack, isLastItem)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(
            result: data,
            callBack: AdapterCallback,
            isLastItem: Boolean
        ) {
            val deviceName = itemView.findViewById(R.id.tv_name) as TextView
            val deviceLastTime = itemView.findViewById(R.id.tvDeviceLastTime) as TextView
            val ivMoreIcon = itemView.findViewById(R.id.ivMoreIcon) as ImageView
            val dividerView = itemView.findViewById(R.id.dividerView) as View
            deviceName.text = result.device_model + "-" + result.device_location
            deviceLastTime.text = result.created_at
            ivMoreIcon.setOnClickListener {
                callBack.itemClick(result)
            }
            if (isLastItem) {
                dividerView.visibility = View.GONE
            } else {
                dividerView.visibility = View.VISIBLE
            }
        }
    }
}

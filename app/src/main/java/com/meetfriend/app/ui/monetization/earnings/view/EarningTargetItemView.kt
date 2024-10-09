package com.meetfriend.app.ui.monetization.earnings.view

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.meetfriend.app.R
import com.meetfriend.app.api.monetization.model.TargetInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemViewEarningTargetBinding
import com.meetfriend.app.utils.FileUtils
import java.util.*

class EarningTargetItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ItemViewEarningTargetBinding? = null
    private lateinit var earningsAdapter: EarningsAdapter


    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.item_view_earning_target, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemViewEarningTargetBinding.bind(view.rootView)

        earningsAdapter = EarningsAdapter(context)

    }

    fun bind(info: TargetInfo) {
        binding?.apply {
            if(FileUtils.isStringToday(info.date.toString())) {
              tvTargetDate.text = context.getString(R.string.label_today)
           } else {
               tvTargetDate.text = convertDateFormat(info.date.toString())
           }

            rvTargetData.apply {
                layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = earningsAdapter
            }
            earningsAdapter.listOfDataItems = info.target
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    private fun convertDateFormat(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-d", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM, yyyy - EEEE", Locale.getDefault())
        val date: Date = inputFormat.parse(inputDate) ?: return ""
        return outputFormat.format(date)
    }


}
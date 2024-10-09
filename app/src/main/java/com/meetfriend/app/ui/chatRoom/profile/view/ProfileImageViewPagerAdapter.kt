package com.meetfriend.app.ui.chatRoom.profile.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import java.util.*


class ProfileImageViewPagerAdapter(val context: Context, val imageList: List<ProfileItemInfo>) :
    PagerAdapter() {

    override fun getCount(): Int {
        return imageList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as RelativeLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView: View =
            mLayoutInflater.inflate(R.layout.image_view_pager_item, container, false)
        val imageView: ImageView = itemView.findViewById<View>(R.id.ivProfileImage) as ImageView


        var imageItem = imageList.get(position)

        Glide.with(context)
            .load(imageItem.filePath)
            .into(imageView)

        //imageView.setImageResource(imageList.get(position))
        Objects.requireNonNull(container).addView(itemView)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }
}
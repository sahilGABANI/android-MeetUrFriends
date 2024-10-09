package com.meetfriend.app.ui.mediapicker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.meetfriend.app.R
import com.meetfriend.app.ui.challenge.AddNewChallengeActivity
import com.meetfriend.app.ui.home.create.AddNewPostActivity
import com.meetfriend.app.ui.main.story.AddStoryActivity
import com.meetfriend.app.ui.mediapicker.custom.PressedImageView
import com.meetfriend.app.ui.mediapicker.interfaces.VideoListByAlbumAdapterCallback
import com.meetfriend.app.ui.mediapicker.model.VideoModel

class VideoListByAlbumAdapter(
    private val context: Context,
    private val videoModelArrayList: ArrayList<VideoModel>,
    private val videoListByAlbumAdapterCallback: VideoListByAlbumAdapterCallback,
) : RecyclerView.Adapter<VideoListByAlbumAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_list_by_album, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return videoModelArrayList.size
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val videoModel = videoModelArrayList[position]

        if (context is AddNewPostActivity) {
            if (AddNewPostActivity.isVideoSelected(videoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.ic_people_unselected)
            } else {
                holder.ivSelector.setImageResource(R.drawable.ic_people_selected)
            }
        } else if (context is AddStoryActivity) {
            if (AddStoryActivity.isVideoSelected(videoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.ic_people_unselected)
            } else {
                holder.ivSelector.setImageResource(R.drawable.ic_people_selected)
            }
        } else if (context is AddNewChallengeActivity) {
            if (AddNewChallengeActivity.isVideoSelected(videoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.ic_people_unselected)
            } else {
                holder.ivSelector.setImageResource(R.drawable.ic_people_selected)
            }
        }

        Glide.with(context)
            .load(videoModel.filePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivVideo)
    }

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val ivVideo: PressedImageView = view.findViewById(R.id.ivVideo)
        val ivSelector: AppCompatImageView = view.findViewById(R.id.ivSelector)

        init {
            ivVideo.setOnClickListener(this)
            ivSelector.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            v?.let {
                videoListByAlbumAdapterCallback.onVideoItemClick(adapterPosition)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}

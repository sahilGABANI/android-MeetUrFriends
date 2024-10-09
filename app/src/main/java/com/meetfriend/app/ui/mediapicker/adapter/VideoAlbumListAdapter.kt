package com.meetfriend.app.ui.mediapicker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.meetfriend.app.R
import com.meetfriend.app.ui.mediapicker.interfaces.VideoAlbumAdapterCallback
import com.meetfriend.app.ui.mediapicker.model.AlbumVideoModel

class VideoAlbumListAdapter(
    private val context: Context,
    private val albumVideoItemArrayList: ArrayList<AlbumVideoModel>,
    private val videoAlbumAdapterCallback: VideoAlbumAdapterCallback
) : RecyclerView.Adapter<VideoAlbumListAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            LayoutInflater.from(context).inflate(R.layout.item_album_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return albumVideoItemArrayList.size
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val albumItem = albumVideoItemArrayList[position]

        Glide.with(context)
            .load(albumItem.albumUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivAlbumCover)

        holder.tvAlbumName.text = albumItem.albumName
        holder.tvAlbumPhotosCount.text = albumItem.videoModelArrayList.size.toString()

        if (albumItem.isSelected) {
            holder.ivSelected.visibility = View.VISIBLE
        } else {
            holder.ivSelected.visibility = View.INVISIBLE
        }
    }

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view) {

        var ivAlbumCover = view.findViewById(R.id.ivAlbumCover) as AppCompatImageView
        var tvAlbumName = view.findViewById(R.id.tvAlbumName) as AppCompatTextView
        var tvAlbumPhotosCount = view.findViewById(R.id.tvAlbumPhotosCount) as AppCompatTextView
        var ivSelected = view.findViewById(R.id.ivSelected) as AppCompatImageView

        init {
            view.setOnClickListener {
                videoAlbumAdapterCallback.onVideoAlbumItemClick(adapterPosition)
                for (i in 0 until albumVideoItemArrayList.size) {
                    albumVideoItemArrayList[i].isSelected = false
                }
                albumVideoItemArrayList[adapterPosition].isSelected = true
                notifyDataSetChanged()
            }
        }
    }
}

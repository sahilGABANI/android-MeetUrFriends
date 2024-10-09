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
import com.meetfriend.app.ui.mediapicker.interfaces.PhotoAlbumAdapterCallback
import com.meetfriend.app.ui.mediapicker.model.AlbumPhotoModel

class PhotoAlbumListAdapter(
    private val context: Context,
    private val albumPhotoItemArrayList: ArrayList<AlbumPhotoModel>,
    private val photoAlbumAdapterCallback: PhotoAlbumAdapterCallback
) : RecyclerView.Adapter<PhotoAlbumListAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            LayoutInflater.from(context).inflate(R.layout.item_album_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return albumPhotoItemArrayList.size
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val albumItem = albumPhotoItemArrayList[position]

        Glide.with(context)
            .load(albumItem.albumUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivAlbumCover)

        holder.tvAlbumName.text = albumItem.albumName
        holder.tvAlbumPhotosCount.text = albumItem.photoModelArrayList.size.toString()

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
                photoAlbumAdapterCallback.onPhotoAlbumItemClick(adapterPosition)
                for (i in 0 until albumPhotoItemArrayList.size) {
                    albumPhotoItemArrayList[i].isSelected = false
                }
                albumPhotoItemArrayList[adapterPosition].isSelected = true
                notifyDataSetChanged()
            }
        }
    }
}
package com.meetfriend.app.ui.mediapicker.model

import java.io.Serializable

class AlbumVideoModel : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    var albumName = ""
    var albumUrl = ""
    var videoModelArrayList = ArrayList<VideoModel>()
    var isSelected = false
}

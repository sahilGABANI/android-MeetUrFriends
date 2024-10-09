package com.meetfriend.app.ui.mediapicker.model

import java.io.Serializable

class AlbumPhotoModel : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    var albumName = ""
    var albumUrl = ""
    var photoModelArrayList = ArrayList<PhotoModel>()
    var isSelected = false
}

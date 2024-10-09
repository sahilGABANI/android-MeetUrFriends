package com.meetfriend.app.ui.mediapicker.model

import java.io.Serializable

class VideoModel : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    var fileName: String? = null
    var filePath: String? = null
    var mimeType: String? = null
    var videoWidth = 0
    var videoHeight = 0
    var fileSize: Long = 0
    var duration = ""
    var dateModifiedInMillis: Long = 0
    var isSelected = false
}

package com.meetfriend.app.utils

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest

object FunctionsUtils {
    fun getImageDimensions(context: Context, imageUrl: String, callback: (width: Int, height: Int) -> Unit) {
        val imageLoader = ImageLoader.Builder(context)
            .build()

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .target { drawable ->
                val width = drawable.intrinsicWidth
                val height = drawable.intrinsicHeight
                callback(width, height)
            }
            .build()
        imageLoader.enqueue(request)
    }
}
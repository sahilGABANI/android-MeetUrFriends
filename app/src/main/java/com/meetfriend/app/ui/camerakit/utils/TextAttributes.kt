package com.meetfriend.app.ui.camerakit.utils

import android.graphics.drawable.Drawable

data class TextAttributes(
    val text: String,
    val color: Int,
    val size: Float,
    val alignment: String,
    val background: Drawable?,
    val fontId: Int,
    val shadowLayerRadius: Float,
    val shadowLayerDx: Float,
    val shadowLayerDy: Float,
    val shadowLayerColor: Int,
)

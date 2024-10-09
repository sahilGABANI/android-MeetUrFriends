package com.meetfriend.app.ui.mediapicker.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import com.meetfriend.app.R
import com.meetfriend.app.ui.mediapicker.model.AlbumPhotoModel
import com.meetfriend.app.ui.mediapicker.model.PhotoModel
import com.meetfriend.app.utils.FileConstants
import timber.log.Timber
import java.io.File

object PhotoUtils {

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    const val NINETY = 90
    const val TWO_HUNDRED_SEVENTY = 270

    @SuppressLint("Range")
    fun loadAlbumsWithPhotoList(
        context: Context,
        onAlbumLoad: (albumPhotoModelArrayList: ArrayList<AlbumPhotoModel>) -> Unit = {}
    ) {
        albumWithPhotoArrayList = ArrayList()

        val contentResolver: ContentResolver = context.contentResolver

        val projectionList: MutableList<String> = ArrayList()
        projectionList.add(MediaStore.Files.FileColumns._ID)
        projectionList.add(MediaStore.MediaColumns.DATA)
        projectionList.add(MediaStore.MediaColumns.DISPLAY_NAME)
        projectionList.add(MediaStore.MediaColumns.DATE_MODIFIED)
        projectionList.add(MediaStore.MediaColumns.MIME_TYPE)
        projectionList.add(MediaStore.MediaColumns.WIDTH)
        projectionList.add(MediaStore.MediaColumns.HEIGHT)
        projectionList.add(MediaStore.MediaColumns.SIZE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            projectionList.add(MediaStore.MediaColumns.ORIENTATION)
        }

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projectionList.toTypedArray(),
            null,
            null,
            MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        )

        val allPhotosAlbumName = context.getString(R.string.label_all_photos)
        val allPhotoModelArrayList = ArrayList<PhotoModel>()

        val aModel = AlbumPhotoModel()
        aModel.albumName = allPhotosAlbumName
        aModel.photoModelArrayList = allPhotoModelArrayList
        albumWithPhotoArrayList.add(aModel)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                var albumNameCol = -1
                var orientationCol = -1

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    albumNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    orientationCol = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
                }

                do {
                    val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                    val path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
                    val name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    val dateTime = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED))
                    val type = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
                    val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))
                    val file = File(path)
                    if (path.isNullOrEmpty() || type.isNullOrEmpty() || !file.isFile) {
                        continue
                    }
                    var width: Int
                    var height: Int
                    var orientation = 0

                    val isVideo = type.contains(FileConstants.MediaTypeVideo)
                    if (!isVideo) {
                        if (path.endsWith(FileConstants.MediaTypeGif) || type.endsWith(FileConstants.MediaTypeGif)) {
                            continue
                        }
                        if (orientationCol != -1) {
                            orientation = cursor.getInt(orientationCol)
                        }
                        width = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH))
                        height = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT))
                        if (width == 0 || height == 0) {
                            val options = BitmapFactory.Options()
                            options.inJustDecodeBounds = true
                            BitmapFactory.decodeFile(path, options)
                            width = options.outWidth
                            height = options.outHeight
                        }
                        if (orientation == NINETY || orientation == TWO_HUNDRED_SEVENTY) {
                            val temp = width
                            width = height
                            height = temp
                        }
                        if (width < FileConstants.AlbumPhotoMinWidth || height < FileConstants.AlbumPhotoMinHeight) {
                            continue
                        }

                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri("external"), id)

                        val photoModel = PhotoModel()
                        photoModel.name = name
                        photoModel.uri = uri
                        photoModel.path = path
                        photoModel.time = dateTime
                        photoModel.width = width
                        photoModel.height = height
                        photoModel.orientation = orientation
                        photoModel.size = size
                        photoModel.duration = 0
                        photoModel.type = type

                        if (albumNameCol != -1) {
                            val albumName = cursor.getString(albumNameCol)?.let { it } ?: ""
                            if (albumName.isNotEmpty()) {
                                val mPos = getAlbumPosFromAlbumList(albumName)
                                if (mPos != -1) {
                                    albumWithPhotoArrayList[mPos].photoModelArrayList.add(photoModel)
                                } else {
                                    val albumModel = AlbumPhotoModel()
                                    albumModel.albumName = albumName
                                    albumModel.albumUrl = path
                                    albumModel.photoModelArrayList.add(photoModel)
                                    albumWithPhotoArrayList.add(albumModel)
                                }
                            }
                        }
                        allPhotoModelArrayList.add(photoModel)
                    }
                } while (cursor.moveToNext())
                cursor.close()

                val mPos = getAlbumPosFromAlbumList(allPhotosAlbumName)
                if (mPos != -1) {
                    albumWithPhotoArrayList[mPos].photoModelArrayList = allPhotoModelArrayList
                }

                val size = albumWithPhotoArrayList.size
                Timber.tag("<><>").e(size.toString())
            }
        }
        onAlbumLoad.invoke(albumWithPhotoArrayList)
    }

    private fun getAlbumPosFromAlbumList(albumName: String?): Int {
        for (i in 0 until albumWithPhotoArrayList.size) {
            if (albumWithPhotoArrayList[i].albumName == albumName) {
                return i
            }
        }
        return -1
    }
}

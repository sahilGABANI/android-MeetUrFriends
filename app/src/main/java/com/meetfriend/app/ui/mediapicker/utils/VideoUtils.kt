package com.meetfriend.app.ui.mediapicker.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import com.meetfriend.app.R
import com.meetfriend.app.ui.mediapicker.model.AlbumVideoModel
import com.meetfriend.app.ui.mediapicker.model.VideoModel
import com.meetfriend.app.utils.FileConstants
import com.meetfriend.app.utils.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

object VideoUtils {

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()

    @SuppressLint("Range")
    fun loadAlbumsWithVideoList(
        context: Context,
        onAlbumLoad: (videoModelArrayList: ArrayList<AlbumVideoModel>) -> Unit = {}
    ) {
        albumWithVideoArrayList = ArrayList()

        val contentResolver: ContentResolver = context.contentResolver

        val projectionList: MutableList<String> = ArrayList()
        projectionList.add(MediaStore.Files.FileColumns._ID)
        projectionList.add(MediaStore.Video.Media.DATA)
        projectionList.add(MediaStore.Video.Media.DISPLAY_NAME)
        projectionList.add(MediaStore.Video.Media.DATE_MODIFIED)
        projectionList.add(MediaStore.Video.Media.MIME_TYPE)
        projectionList.add(MediaStore.Video.Media.WIDTH)
        projectionList.add(MediaStore.Video.Media.HEIGHT)
        projectionList.add(MediaStore.Video.Media.SIZE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            projectionList.add(MediaStore.Video.Media.DURATION)
        }

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projectionList.toTypedArray(),
            null,
            null,
            MediaStore.Video.Media.DATE_MODIFIED + " DESC"
        )

        val allPhotosAlbumName = context.getString(R.string.label_all_videos)
        val allVideoModelArrayList = ArrayList<VideoModel>()

        val aModel = AlbumVideoModel()
        aModel.albumName = allPhotosAlbumName
        aModel.videoModelArrayList = allVideoModelArrayList
        albumWithVideoArrayList.add(aModel)

        val mMetadataRetriever = MediaMetadataRetriever()
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    var albumNameCol = -1

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        albumNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    }

                    do {
                        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        val name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                        val dateTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED))
                        val type = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE))
                        var height = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT))
                        var width = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH))
                        val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))

                        if (path.isNullOrEmpty() || type.isNullOrEmpty()) {
                            continue
                        }

                        val file = File(path)
                        if (!file.isFile) {
                            continue
                        }
                        val isVideo = type.contains(FileConstants.MediaTypeVideo)
                        if (isVideo) {
                            if (width == 0 || height == 0) {
                                width = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH))
                                height = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT))
                                if (width == 0 || height == 0) {
                                    val options = BitmapFactory.Options()
                                    options.inJustDecodeBounds = true
                                    BitmapFactory.decodeFile(path, options)
                                    width = options.outWidth
                                    height = options.outHeight
                                }
                                if (width < FileConstants.VideoMinWidth || height < FileConstants.VideoMinHeight) {
                                    continue
                                }
                            }

                            var duration: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                            } else {
                                mMetadataRetriever.setDataSource(path)
                                mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            }

                            if (duration.isNullOrEmpty()) {
                                duration = "0"
                            }
                            val videoModel = VideoModel()
                            videoModel.fileName = name
                            videoModel.filePath = path
                            videoModel.dateModifiedInMillis = dateTime
                            videoModel.videoWidth = width
                            videoModel.videoHeight = height
                            videoModel.fileSize = size
                            videoModel.duration = FileUtils.getVideoDurationInHourMinSecFormat(duration.toLong())
                            videoModel.mimeType = type

                            if (albumNameCol != -1) {
                                val albumName = cursor.getString(albumNameCol) ?: ""
                                if (albumName.isNotEmpty()) {
                                    val mPos = getAlbumPosFromAlbumList(albumName)
                                    if (mPos != -1) {
                                        albumWithVideoArrayList[mPos].videoModelArrayList.add(videoModel)
                                    } else {
                                        val albumModel = AlbumVideoModel()
                                        albumModel.albumName = albumName
                                        albumModel.albumUrl = path
                                        albumModel.videoModelArrayList.add(videoModel)
                                        albumWithVideoArrayList.add(albumModel)
                                    }
                                }
                            }
                            allVideoModelArrayList.add(videoModel)
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                    mMetadataRetriever.release()

                    val mPos = getAlbumPosFromAlbumList(allPhotosAlbumName)
                    if (mPos != -1) {
                        albumWithVideoArrayList[mPos].videoModelArrayList = allVideoModelArrayList
                    }

                    val size = albumWithVideoArrayList.size
                    Timber.tag("<><>").e(size.toString())
                }
            }
        } catch (e: IOException) {
            Timber.wtf(e.message)
        } finally {
            onAlbumLoad.invoke(albumWithVideoArrayList)
        }
    }

    private fun getAlbumPosFromAlbumList(albumName: String?): Int {
        for (i in 0 until albumWithVideoArrayList.size) {
            if (albumWithVideoArrayList[i].albumName == albumName) {
                return i
            }
        }
        return -1
    }
}

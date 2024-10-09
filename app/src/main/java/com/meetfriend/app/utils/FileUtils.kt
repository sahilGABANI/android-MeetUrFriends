package com.meetfriend.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd
import com.meetfriend.app.R
import com.meetfriend.app.databinding.AdUnifiedBinding
import com.meetfriend.app.newbase.ActivityManager
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*


object FileUtils {

    const val DirRoot = "MeetUrFriend"

    private const val FileTypeAllImage = "image/*"
    private const val FileTypeJPG = "image/jpg"
    private const val FileTypeJPEG = "image/jpeg"
    private const val FileTypePNG = "image/png"
    private const val FileTypeWebp = "image/webp"
    private val allImageMimeTypes = arrayOf(FileTypeJPG, FileTypeJPEG, FileTypePNG, FileTypeWebp)

    const val PICK_IMAGE = 1001
    const val PICK_Video = 2001
    val loginUserTokenSharedPreference: SharedPreferences =  ActivityManager.getInstance().application!!.getSharedPreferences("LOGGED_IN_USER_TOKEN", Context.MODE_PRIVATE)



    fun getPath(context: Context, uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                if ("primary".equals(
                        type,
                        ignoreCase = true
                    )
                ) return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean =
        "com.google.android.apps.photos.content" == uri.authority

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean =
        "com.android.externalstorage.documents" == uri.authority

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean =
        "com.android.providers.downloads.documents" == uri.authority

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean =
        "com.android.providers.media.documents" == uri.authority


    fun openImagePicker(activity: Activity) {
        val intent = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI)
        }
        intent.apply {
            type = FileTypeAllImage
            putExtra(Intent.EXTRA_MIME_TYPES, allImageMimeTypes)
            action = Intent.ACTION_GET_CONTENT
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra("return-data", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, PICK_IMAGE)
    }

    fun openVideoPicker(activity: Activity) {
        val intent = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI)
        }
        intent.apply {
            type = "video/*"
            action = Intent.ACTION_GET_CONTENT
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra("return-data", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, PICK_Video)
    }

    @SuppressLint("NewApi")
    fun String.toDate(dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss", timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Date {
        val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
        parser.timeZone = timeZone
        return parser.parse(this)
    }

    @SuppressLint("NewApi")
    fun String.toDateWithoutTimeZone(dateFormat: String = "yyyy-MM-dd HH:mm:ss"): Date {
        val parser = SimpleDateFormat(dateFormat)
        return parser.parse(this)
    }

    @SuppressLint("NewApi")
    fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(this)
    }


    fun lastSeenMessageTime(date:String):String {
        val lastMessageDate = date.toDate().formatTo("MM-dd-yyyy")
        val lastMessageTime = date.toDate().formatTo("hh:mm a")
        val currentDate: String = DateFormat.format("MM-dd-yyyy", Date().time) as String
        val lastMessageMonthName = date.toDate().formatTo("MMM")
        val dateLastMessage = date.toDate().formatTo("dd").toInt()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayDate = cal.time.formatTo("MM-dd-yyyy")
        val finalTime = if (currentDate == lastMessageDate) {
            "Today, $lastMessageTime"
        } else {
            if (lastMessageDate == yesterdayDate) {
                "Yesterday, $lastMessageTime"
            } else {
                "$lastMessageMonthName $dateLastMessage"
            }
        }
        return finalTime
    }

    fun msgDateTime(date:String):String {
        val lastMessageDate = date.toDate().formatTo("MM-dd-yyyy")
        val currentDate: String = DateFormat.format("MM-dd-yyyy", Date().time) as String
        val lastMessageMonthName = date.toDate().formatTo("MMMM dd, yyyy")
        val weekName = date.toDate().formatTo("EEEE")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayDate = cal.time.formatTo("MM-dd-yyyy")
        val finalTime = if (currentDate == lastMessageDate) {
            "Today"
        } else {
            if (lastMessageDate == yesterdayDate) {
                "Yesterday"
            } else if (isDateInCurrentWeek(lastMessageMonthName)) {
                "$weekName"
            } else {
                "$lastMessageMonthName"
            }
        }
        return finalTime
    }

    fun isDateInCurrentWeek(dateToCheck: String): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val date =  dateFormat.parse(dateToCheck)
        calendar.time = date
        val weekNumberToCheck = calendar.get(Calendar.WEEK_OF_YEAR)
        return currentWeekNumber == weekNumberToCheck
    }

    fun getVideoDurationInHourMinSecFormat(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val mFormatter = Formatter()
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    fun saveMeetUrFriendsLogo(context: Context) {
        val bm = BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_logo)
        val filePath = getHinoteLogoFile(context)
        if (!filePath.exists()) {
            try {
                val outStream = FileOutputStream(filePath)
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: Exception) {
                Timber.i(e)
            }
        }
    }

    fun getHinoteLogoFile(context: Context): File {
        val cachePath = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DCIM),
            "/MeetUrFriendsLogo/"
        )
        // Create the storage directory if it does not exist
        if (!cachePath.exists()) {
            if (!cachePath.mkdirs()) {
                Timber.d("failed to create directory")
            }
        }
        return File(cachePath, "meeturfriends_title.png")
    }

    fun findWordForRightHanded(
        str: String,
        offset: Int
    ): String { // when you touch ' ', this method returns left word.
        var offset = offset
        if (str.length == offset) {
            offset-- // without this code, you will get exception when touching end of the text
        }
        if (str[offset] == ' ') {
            offset--
        }
        var startIndex = offset
        var endIndex = offset
        try {
            while (str[startIndex] != ' ' && str[startIndex] != '\n') {
                startIndex--
            }
        } catch (e: StringIndexOutOfBoundsException) {
            startIndex = 0
        }
        try {
            while (str[endIndex] != ' ' && str[endIndex] != '\n') {
                endIndex++
            }
        } catch (e: StringIndexOutOfBoundsException) {
            endIndex = str.length
        }

        // without this code, you will get 'here!' instead of 'here'
        // if you use only english, just check whether this is alphabet,
        // but 'I' use korean, so i use below algorithm to get clean word.
        val last = str[endIndex - 1]
        if (last == ',' || last == '.' || last == '!' || last == '?' || last == ':' || last == ';') {
            endIndex--
        }
        return str.substring(startIndex, endIndex)
    }

    fun getWeekDates(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.time = today
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val sunday = calendar.time

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val saturday = calendar.time

        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val mondayFormatted = formatter.format(sunday)
        val saturdayFormatted = formatter.format(saturday)

        return Pair(mondayFormatted, saturdayFormatted)
    }

    fun getPreviousWeekDates(): Pair<String, String> {
        val calendar = Calendar.getInstance()

        val today = calendar.time
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.time = today
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val previousSunday = calendar.time

        // Set the calendar to Saturday of the previous week
        calendar.add(Calendar.DAY_OF_YEAR, 6) // 6 days to get to Saturday
        val previousSaturday = calendar.time

        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val previousMondayFormatted = formatter.format(previousSunday)
        val previousSaturdayFormatted = formatter.format(previousSaturday)

        return Pair(previousMondayFormatted, previousSaturdayFormatted)
    }

    fun convertDateFormat(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-d", Locale.getDefault())
        val date: Date = inputFormat.parse(inputDate) ?: return ""
        return outputFormat.format(date)
    }

    fun convertGiftDateFormat(inputDate: String ?): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date: Date = inputFormat.parse(inputDate) ?: return ""
        return outputFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun isStringToday(dateString: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        val calendar = Calendar.getInstance()
        val today = calendar.time
        return date != null && SimpleDateFormat("yyyy-MM-dd").format(date) == SimpleDateFormat("yyyy-MM-dd").format(
            today
        )
    }

    fun redeemHistoryTime(date: String): String {
        val lastMessageDate = date.toDate().formatTo("MM-dd-yyyy")
        val lastMessageTime = date.toDate().formatTo("hh:mm a")
        val currentDate: String = DateFormat.format("MM-dd-yyyy", Date().time) as String
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayDate = cal.time.formatTo("MM-dd-yyyy")
        val finalTime = if (currentDate == lastMessageDate) {
            "Today: $lastMessageTime"
        } else {
            if (lastMessageDate == yesterdayDate) {
                "Yesterday: $lastMessageTime"
            } else {
                val inputFormat =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date: Date = inputFormat.parse(date)
                outputFormat.format(date)
            }
        }
        return finalTime
    }

    fun manageRedeem(context: Context,textview: AppCompatTextView, redeem: AppCompatTextView) {
        val cal: Calendar = Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("dd")
        val smf = java.text.SimpleDateFormat("MMM")
        val date = sdf.format(cal.time)
        val month = smf.format(cal.time)
        cal.add(Calendar.MONTH, 1)
        val nextMonth = smf.format(cal.time)

        if (date.toInt() != 1 && date.toInt() != 15) {
            if (date.toInt() > 15) {
                textview.text =
                    context.getString(R.string.label_you_can_redeem_by).plus(" ").plus("1st").plus(" ")
                        .plus(nextMonth)
            } else {
                textview.text =
                    context.getString(R.string.label_you_can_redeem_by).plus(" ").plus("15th").plus(" ")
                        .plus(month)
            }
            redeem.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.transperent_purple);
            val backgroundDrawable =
                ContextCompat.getDrawable(context, R.drawable.button_date_filter_bg)
            redeem.background = backgroundDrawable
            redeem.isEnabled = false

        } else {
            textview.isVisible = false
            redeem.isEnabled = true
        }
    }

    fun loadBannerAd(context: Context,view:FrameLayout,adId:String){
        val mAdView = AdView(context)
        mAdView.setAdSize(AdSize.BANNER)
        mAdView.adUnitId = adId
        view.addView(mAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    fun loadNativeAd(nativeAd: NativeAd, unifiedAdBinding: AdUnifiedBinding) {
        val nativeAdView = unifiedAdBinding.root

        nativeAdView.mediaView = unifiedAdBinding.adMedia
        nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = unifiedAdBinding.adBody
        nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
        nativeAdView.iconView = unifiedAdBinding.adAppIcon
        nativeAdView.priceView = unifiedAdBinding.adPrice
        nativeAdView.starRatingView = unifiedAdBinding.adStars
        nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

        unifiedAdBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.setMediaContent(it) }

        if (nativeAd.body == null) {
            unifiedAdBinding.adBody.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adBody.visibility = View.VISIBLE
            unifiedAdBinding.adBody.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            unifiedAdBinding.adCallToAction.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adCallToAction.visibility = View.VISIBLE
            unifiedAdBinding.adCallToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            unifiedAdBinding.adAppIcon.visibility = View.GONE
        } else {
            unifiedAdBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            unifiedAdBinding.adAppIcon.visibility = View.VISIBLE
        }

        if (nativeAd.starRating == null) {
            unifiedAdBinding.adStars.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStars.rating = nativeAd.starRating!!.toFloat()
            unifiedAdBinding.adStars.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            unifiedAdBinding.adAdvertiser.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adAdvertiser.text = nativeAd.advertiser
            unifiedAdBinding.adAdvertiser.visibility = View.VISIBLE
        }

        nativeAdView.setNativeAd(nativeAd)
    }

}
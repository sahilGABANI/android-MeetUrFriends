package com.meetfriend.app.ui.storywork

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.abedelazizshe.lightcompressorlibrary.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityStoryDetailBinding
import com.meetfriend.app.network.API
import com.meetfriend.app.network.ServerRequest
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.responseclasses.ChatPOJO
import com.meetfriend.app.responseclasses.CommonDataModel
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewModel
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewState
import com.meetfriend.app.ui.storylibrary.StoriesProgressView
import com.meetfriend.app.ui.storywork.models.Stories
import com.meetfriend.app.ui.storywork.models.StoryDetailResponse
import com.meetfriend.app.ui.storywork.models.StoryDetailResult
import com.meetfriend.app.utilclasses.AppConstants
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utilclasses.UtilsClass.FIREBASE_NODE
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import contractorssmart.app.utilsclasses.PreferenceHandler.readString
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.utils.ContentUriUtils
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlin.properties.Delegates

class StoryDetailActivity : AppCompatActivity(), StoriesProgressView.StoriesListener {

    lateinit var binding: ActivityStoryDetailBinding
    private var counter = 0
    lateinit var resources: MutableList<Stories>
    var pressTime = 0L
    var limit = 500L

    private lateinit var stories: Stories
    lateinit var id: String
    lateinit var type: RequestBody
    var size by Delegates.notNull<Int>()
    lateinit var fireMessages: DatabaseReference
    lateinit var fireMessages_Other: DatabaseReference
    private var FIREBASE_MESSAGES = "messages"
    private var mainNode = ""
    var STORY = "99"
    private var user_id: String = ""
    private var FIREBASE_DATA = "data"
    private var FIREBASE_USERS = "users"
    private var userName: String = ""

    private var otherUserName: String = ""
    private var userImage: String = ""
    private var otherUserImage: String = ""

    private var FIREBASE_LASTMSG = "lastMSG"
    private var FIREBASE_LASTMSG_TIME = "lastmsgTIME"
    private var storyTime: String = ""

    private val SECOND_MILLIS: Int = 1000
    private val MINUTE_MILLIS: Int = 60 * SECOND_MILLIS
    private val HOUR_MILLIS: Int = 60 * MINUTE_MILLIS
    var reportDialog: Dialog? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MainHomeViewModel>
    private lateinit var mainHomeViewModel: MainHomeViewModel


    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    val picsList: ArrayList<String> = ArrayList()
    private val arrayList: ArrayList<Uri> = ArrayList()

    private val onTouchListener: View.OnTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    binding.stories.pause()
                    return false
                }

                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    binding.stories.resume()
                    return limit < now - pressTime
                }
            }
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_story_detail)
        mainHomeViewModel = getViewModelFromFactory(viewModelFactory)
        val window: Window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("#000000")
        binding.cross.setOnClickListener {
            RxBus.publish(RxEvent.StoryWatched)
            finish()
        }

        listenToViewModel()
        if (intent.getBooleanExtra("fromchatscreen", false)) {
            size = 1
            if (intent.getBooleanExtra("right", false)) {
                val userImage = intent.getStringExtra("image")
                val stoaryImage = intent.getStringExtra("storyimage")
                val name = intent.getStringExtra("name")
                val timestamp = intent.getStringExtra("timestamp")
                val storytype = intent.getStringExtra("storytype")
                if (timestamp != null) {
                    storyTime = TimeAgo.getTimeAgo(timestamp.toLong())
                }
                user_id = readString(this, "USER_ID", "")
                id = intent.getStringExtra("userId").toString()
                makeNode()

                if (userImage?.isNotBlank() == true && userImage?.isNotEmpty() == true) Picasso.get().load(userImage)
                    .into(binding.profileimage, object : Callback {
                        override fun onSuccess() {
                            binding.progressBar.visibility = View.GONE
                        }

                        override fun onError(e: Exception?) {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@StoryDetailActivity, "" + e?.message, Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                binding.name.text = name
                binding.stories.setStoriesCount(1)
                binding.stories.setStoriesListener(this@StoryDetailActivity)
                binding.stories.startStories()
                if (stoaryImage != null) {
                    if (storytype == "image") {
                        binding.stories.setStoryDuration(5000L)
                        showImage(stoaryImage, storyTime)
                    } else {
                        binding.stories.setStoryDuration(60000L)
                        startVideo(stoaryImage, storyTime)
                    }
                }
                otherUserName = name!!
                if (userImage != null) {
                    otherUserImage = userImage
                }
                addUser()
                addFirebaseListener()
            } else {
                binding.boottom.visibility = View.VISIBLE
                binding.menu.visibility = View.GONE
                binding.stories.setStoriesCount(1)
                binding.stories.setStoriesListener(this)
                binding.stories.startStories()
                userImage = readString(this, "PROFILE_PHOTO", "")
                val stoaryImage = intent.getStringExtra("storyimage")
                val timestamp = intent.getStringExtra("timestamp")
                val storytype = intent.getStringExtra("storytype")
                if (timestamp != null) {
                    storyTime = TimeAgo.getTimeAgo(timestamp.toLong())
                }
                binding.stories.setStoriesCount(1)
                binding.stories.setStoriesListener(this@StoryDetailActivity)
                binding.stories.startStories()
                if (stoaryImage != null) {
                    if (storytype == "image") {
                        binding.stories.setStoryDuration(5000L)
                        showImage(stoaryImage, storyTime)
                    } else {
                        binding.stories.setStoryDuration(60000L)
                        startVideo(stoaryImage, storyTime)
                    }
                }

                if (userImage?.isNotBlank() == true && userImage?.isNotEmpty() == true) Picasso.get().load(userImage)
                    .into(binding.profileimage, object : Callback {
                        override fun onSuccess() {
                            binding.progressBar.visibility = View.GONE
                        }

                        override fun onError(e: Exception?) {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@StoryDetailActivity, "" + e?.message, Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                userName = readString(this, "FIRSTNAME", "") + " " + readString(this, "LASTNAME", "")

                binding.name.text = userName

            }
        } else {
            if (intent.getBooleanExtra("cuser", false)) {
                binding.replyview.visibility = View.VISIBLE
                binding.addnew.visibility = View.GONE
                binding.deleteview.visibility = View.GONE
            } else {
                binding.replyview.visibility = View.VISIBLE
                binding.deleteview.visibility = View.GONE
            }
            user_id = readString(this, "USER_ID", "")
            id = intent.getIntExtra("userId", 0).toString()

            id.let {
                mainHomeViewModel.getUserStory(id)
            }

            if (intent.getBooleanExtra("cuser", false)) {

            } else {
                mainNode = if (user_id.toInt() < id.toInt()) user_id + "_" + id else id + "_" + user_id
                userName = readString(this, "FIRSTNAME", "") + " " + readString(this, "LASTNAME", "")
                userImage = readString(this, "PROFILE_PHOTO", "")
            }
        }
        initListeners()
    }

    private fun listenToViewModel() {
        mainHomeViewModel.mainHomeState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is MainHomeViewState.ErrorMessage -> {

                }

                is MainHomeViewState.StoryLoadingState -> {

                }

                is MainHomeViewState.SuccessMessage -> {

                }

                is MainHomeViewState.StoryReportState -> {
                    showToast(state.successMessage)
                    reportDialog?.dismiss()
                }

                is MainHomeViewState.StoryDetailsState -> {
                    startStories(state.storyDetailResult)
                    setProfile(state.storyDetailResult.storiesResult)
                }

                else -> {

                }
            }
        }
    }

    private fun makeNode() {
        mainNode = if (user_id.toInt() < id.toInt()) user_id + "_" + id else id + "_" + user_id
        userName = readString(this, "FIRSTNAME", "") + " " + readString(this, "LASTNAME", "")
        userImage = readString(this, "PROFILE_PHOTO", "")
    }

    private fun setProfile(storiesResult: StoryDetailResult) {
        binding.name.text = storiesResult.firstName + " " + storiesResult.lastName
        Picasso.get().load(storiesResult.profile_photo).placeholder(
                R.drawable.place_holder_image
            ).into(binding.profileimage)
    }

    private fun initListeners() {

        binding.reverse.setOnClickListener {
            binding.stories.reverse()
        }
        binding.skip.setOnClickListener {
            binding.stories.skip()
        }
        binding.reverse.setOnTouchListener(onTouchListener)
        binding.skip.setOnTouchListener(onTouchListener)

        binding.delettext.setOnClickListener {
            if (this::stories.isInitialized) {
                if (resources.size == 1) {
                    mainHomeViewModel.deleteStory(stories.id.toString())
                    finish()
                    resources.clear()
                } else {
                    mainHomeViewModel.deleteStory(stories.id.toString())
                    finish()
                }
            }
        }

        binding.report.setOnClickListener {
            if (this::stories.isInitialized) {
                Handler(Looper.myLooper()!!).post { binding.stories.pause() }
                showReportDialog(stories.id.toString())
            }
        }

        binding.edittext.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.stories.pause()
            } else {
                binding.stories.resume()
            }
        }
        binding.edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(abc: Editable?) {
                sendMessage(abc.toString())
            }
        })

        KeyboardVisibilityEvent.setEventListener(this) {
            if (!it) {
                binding.edittext.clearFocus()
                binding.stories.resume()
            }
        }
        binding.menu.setOnClickListener {
            if (intent.getBooleanExtra("cuser", false)) {
                if (intent.getIntExtra("userId", -1) == loggedInUserCache.getLoggedInUserId()) {
                    if (binding.deleteview.visibility == View.VISIBLE) {
                        binding.deleteview.visibility = View.GONE
                    } else {
                        binding.deleteview.visibility = View.VISIBLE
                    }
                } else {
                    if (binding.report.visibility == View.VISIBLE) {
                        binding.report.visibility = View.GONE
                    } else {
                        binding.report.visibility = View.GONE
                        binding.report.visibility = View.VISIBLE
                    }
                }
            } else {
                binding.deleteview.visibility = View.GONE
                binding.addnew.visibility = View.GONE
                if (binding.replyview.visibility == View.VISIBLE) {
                    binding.replyview.visibility = View.GONE
                    binding.report.visibility = View.VISIBLE
                } else {
                    binding.replyview.visibility = View.VISIBLE
                    binding.report.visibility = View.GONE
                }
            }
        }

        binding.addnew.setOnClickListener {
            Handler(Looper.myLooper()!!).post { binding.stories.pause() }
            FilePickerBuilder.instance.setMaxCount(1).enableVideoPicker(true).enableImagePicker(true).setActivityTheme(R.style.LibAppThemeCustom)
                .enableCameraSupport(true).pickPhoto(this, 1)
        }

    }

    private fun showReportDialog(id: String) {
        if (reportDialog == null) {
            reportDialog = Dialog(this, R.style.full_screen_dialog)
            reportDialog?.setContentView(R.layout.report_post_dialog)
            reportDialog?.setCanceledOnTouchOutside(true)
            reportDialog?.setCancelable(true)
        }

        reportDialog?.show()
        val metrics: DisplayMetrics = getResources().displayMetrics
        val width: Int = metrics.widthPixels
        reportDialog?.window?.setLayout((6 * width) / 7, RelativeLayout.LayoutParams.WRAP_CONTENT)
        val ivClose: ImageView = reportDialog?.findViewById(R.id.ivClose) as ImageView
        val etReason: EditText = reportDialog?.findViewById(R.id.etReason) as EditText
        val tvSendReport: TextView = reportDialog?.findViewById(R.id.tvSendReport) as TextView
        etReason.visibility = View.GONE
        tvSendReport.visibility = View.GONE
        ivClose.setOnClickListener {
            Handler(Looper.myLooper()!!).post { binding.stories.resume() }
            reportDialog!!.dismiss()
        }

        val tvNudity: TextView = reportDialog?.findViewById(R.id.tvNudity) as TextView
        tvNudity.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvNudity.text.toString().trim())
        }
        val tvViolence: TextView = reportDialog!!.findViewById(R.id.tvViolence) as TextView
        tvViolence.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvViolence.text.toString().trim())
        }
        val tvSpam: TextView = reportDialog!!.findViewById(R.id.tvSpam) as TextView
        tvSpam.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvSpam.text.toString().trim())
        }
        val tvFake: TextView = reportDialog!!.findViewById(R.id.tvFake) as TextView
        tvFake.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvFake.text.toString().trim())
        }
        val tvHarrasment: TextView = reportDialog!!.findViewById(R.id.tvHarrasment) as TextView
        tvHarrasment.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvHarrasment.text.toString().trim())
        }
        val tvhate: TextView = reportDialog!!.findViewById(R.id.tvhate) as TextView
        tvhate.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvhate.text.toString().trim())
        }
        val tvSuicide: TextView = reportDialog!!.findViewById(R.id.tvSuicide) as TextView
        tvSuicide.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvSuicide.text.toString().trim())
        }
        val tvTerrorism: TextView = reportDialog!!.findViewById(R.id.tvTerrorism) as TextView
        tvTerrorism.setOnClickListener {
            reaportApiCAll(stories.id.toString(), tvTerrorism.text.toString().trim())
        }
        val tvOther: TextView = reportDialog!!.findViewById(R.id.tvOther) as TextView
        tvOther.setOnClickListener {
            etReason.visibility = View.VISIBLE
            tvSendReport.visibility = View.VISIBLE
        }
        tvSendReport.setOnClickListener {
            if (etReason.text.toString().trim() == "") {
                etReason.error = "Type a reason to continue"
                return@setOnClickListener
            }
            etReason.error = null
            reaportApiCAll(stories.id.toString(), etReason.text.toString())
        }

    }

    private fun reaportApiCAll(id: String, reason: String) {
        mainHomeViewModel.reportStory(id, reason)
    }

    private fun sendMessage(message: String) {
        binding.send.setOnClickListener {
            if (message.isEmpty()) {
                showToast("Please enter a message")
            } else {
                binding.edittext.clearFocus()
                binding.edittext.setText("")
                UIUtil.hideKeyboard(this)
                sendMessageFlowStart(message)
            }
        }
    }

    private fun sendMessageFlowStart(message: String) {
        val message_root = fireMessages.push().key?.let { fireMessages.child(it) }
        val message_root1: DatabaseReference? = fireMessages.push().key?.let { fireMessages_Other.child(it) }
        var map = HashMap<String?, Any?>()
        map["fromID"] = user_id
        map["toID"] = id
        map["msgTEXT"] = message
        map["msgTIME"] = ServerValue.TIMESTAMP
        map["msgTYPE"] = STORY
        map["file"] = stories.file_path
        map["isSave"] = "0"
        map["isRead"] = "0"
        map["storytype"] = stories.type
        map["timestamp"] = stories.timestamp
        map["senderMsgId"] = message_root1?.key
        message_root?.updateChildren(map)
        map = HashMap()
        map["fromID"] = user_id
        map["toID"] = id
        map["msgTEXT"] = message
        map["msgTIME"] = ServerValue.TIMESTAMP
        map["msgTYPE"] = STORY
        map["file"] = stories.file_path
        map["isSave"] = "0"
        map["isRead"] = "0"
        map["storytype"] = stories.type
        map["timestamp"] = stories.timestamp
        map["senderMsgId"] = message_root?.key
        message_root1?.updateChildren(map)

        val fireTable = FirebaseDatabase.getInstance().reference.child(FIREBASE_NODE).child(mainNode).child(FIREBASE_DATA)
        fireTable.child(FIREBASE_LASTMSG).setValue(message)
        fireTable.child(FIREBASE_LASTMSG_TIME).setValue(ServerValue.TIMESTAMP)
        sendNoti(message, "", "chat")
        binding.edittext.setText("")

    }

    private fun startStories(storyDetailResponse: StoryDetailResponse) {
        if (storyDetailResponse.storiesResult.stories.isNotEmpty()) {
            counter = 0
            resources = mutableListOf()
            resources.addAll(storyDetailResponse.storiesResult.stories)
            binding.stories.setStoriesCount(resources.size)
            size = resources.size
            binding.stories.setStoriesListener(this)
            binding.stories.startStories()

            stories = if (resources[counter].type == "video") {
                resources[counter].file_path?.let {
                    binding.stories.setStoryDuration(60000L)
                    startVideo(it, resources[counter].created_at)
                    mainHomeViewModel.viewStory(resources[counter].id.toString())
                }
                resources[counter]
            } else {
                resources[counter].file_path?.let {
                    binding.stories.setStoryDuration(7000L)
                    showImage(it, resources[counter].created_at)
                    mainHomeViewModel.viewStory(resources[counter].id.toString())
                }
                resources[counter]
            }
        }

    }

    private fun showImage(imageOrVideoUrl: String, createdAt: String) {
        Handler(Looper.myLooper()!!).post { binding.stories.pause() }
        binding.progressBar.visibility = View.VISIBLE
        binding.videoview.visibility = View.GONE
        binding.image.visibility = View.VISIBLE
        binding.time.text = createdAt
        if (imageOrVideoUrl.isNotBlank() && imageOrVideoUrl.isNotEmpty()) Picasso.get().load("$imageOrVideoUrl")
            .into(binding.image, object : Callback {
                override fun onSuccess() {
                    Handler(Looper.myLooper()!!).post {
                        binding.stories.resume()
                    }
                    binding.progressBar.visibility = View.GONE
                }

                override fun onError(e: Exception?) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@StoryDetailActivity, "" + e?.message, Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun startVideo(url: String, createdAt: String) {
        binding.stories.pauseStory(counter)
        binding.progressBar.visibility = View.VISIBLE
        binding.videoview.visibility = View.VISIBLE
        binding.image.visibility = View.GONE
        binding.time.text = createdAt

        binding.videoview.setVideoURI((url).toUri())

        binding.videoview.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setOnInfoListener(MediaPlayer.OnInfoListener { _, i, _ ->
                when (i) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        binding.progressBar.visibility = View.GONE
                        binding.stories.resumeStory(counter)
                        return@OnInfoListener true
                    }

                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.stories.pauseStory(counter)
                        return@OnInfoListener true
                    }

                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.stories.pauseStory(counter)
                        return@OnInfoListener true
                    }

                    MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.stories.pauseStory(counter)
                        return@OnInfoListener true
                    }

                    MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.stories.pauseStory(counter)
                        return@OnInfoListener true
                    }
                }
                false
            })
            binding.videoview.start()
            binding.videoview.requestFocus()
            binding.progressBar.visibility = View.GONE
            binding.stories.resumeStory(counter)
            binding.stories.setDurationStory(mediaPlayer.duration.toLong(), counter)
        }
    }

    override fun onNext() {
        if (this::resources.isInitialized) {
            mainHomeViewModel.viewStory(resources[++counter].id.toString())
            if (counter < size) {
                stories = if (resources[counter].type == "video") {
                    resources[counter].file_path?.let {
                        startVideo(
                            it, resources[counter].created_at
                        )
                    }
                    resources[counter]
                } else {
                    resources[counter].file_path?.let {
                        binding.stories.setStoryDuration(5000L)
                        showImage(it, resources[counter].created_at)
                    }
                    resources[counter]
                }

            }
        } else {
        }
    }

    override fun onPrev() {
        if (this::resources.isInitialized) {
            if ((counter - 1) < 0) return
            stories = if (resources[--counter].file_path?.endsWith("mp4") == true) {
                resources[counter].file_path?.let { startVideo(it, resources[counter].created_at) }
                resources[counter]
            } else {
                resources[counter].file_path?.let { showImage(it, resources[counter].created_at) }
                resources[counter]
            }
        } else {

        }
    }

    override fun onComplete() {
        counter = 0
        RxBus.publish(RxEvent.StoryWatched)
        finish()
    }

    override fun onDestroy() {
        binding.stories.destroy()
        super.onDestroy()
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    fun showLoading() {
        CallProgressWheel.showLoadingDialog(this)
    }

    fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }

    fun showToast(msg: CharSequence) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun addUser() {
        val fireTable = FirebaseDatabase.getInstance().reference.child(FIREBASE_NODE).child(mainNode).child(FIREBASE_DATA).child(FIREBASE_USERS)
        val item = ChatPOJO(
            user_id, id, userName, otherUserName, userImage, otherUserImage
        )
        fireTable.setValue(item)
    }

    private fun addFirebaseListener() {
        fireMessages = FirebaseDatabase.getInstance().reference.child(FIREBASE_NODE).child(mainNode).child(FIREBASE_MESSAGES + "_" + user_id)

        fireMessages_Other = FirebaseDatabase.getInstance().reference.child(FIREBASE_NODE).child(mainNode).child(FIREBASE_MESSAGES + "_" + id)
    }

    private fun sendNoti(from: String, giftId: String, type: String) {
        object : ServerRequest<CommonDataModel?>(
            this, API.sendNoti(id, from, giftId, type), false
        ) {
            override fun onCompletion(
                call: Call<CommonDataModel?>?, response: Response<CommonDataModel?>?
            ) {
                if (response != null) {
                    if (response.body() != null && response.code() == 200) {
                    } else {
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val pathVideo = data.getParcelableArrayListExtra<Uri>(FilePickerConst.KEY_SELECTED_MEDIA)
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    this.getResources().getString(R.string.application_name)
                )
                if (checkIsImage(this, pathVideo?.get(0)!!)) {
                    showLoading(true)
                    val file = File(dir, "image" + System.currentTimeMillis() + ".png")
                    arrayList.add(pathVideo[0])
                    showLoading(true)
                    lifecycleScope.launch {
                        val path = ContentUriUtils.getFilePath(this@StoryDetailActivity, pathVideo[0])
                        Compressor.compress(this@StoryDetailActivity, File(path)) {
                            //resolution(1280, 720)
                            quality(AppConstants.POST_PIC_QUALITY)
                            default()
                            destination(file)
                            picsList.add(file.absolutePath)
                            val handler = Handler(Looper.getMainLooper())
                            val r = Runnable {
                                showLoading(false)
                                addStory()
                            }
                            handler.postDelayed(r, AppConstants.POST_PIC_LOADING.toLong())
                        }
                    }
                } else {
                    val file = File(dir, "image" + System.currentTimeMillis() + ".mp4")
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(
                        ContentUriUtils.getFilePath(
                            this, pathVideo[0]
                        )
                    )
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    if (duration != null) {
                        if (duration / 1000 > 180) {
                            showToast("Video length is greater than 3 minutes")
                        } else {
                            showToast("Compressing video, Please wait...")
                            MediaVideoCompressor.start(
                                ContentUriUtils.getFilePath(this, pathVideo[0])!!, file.absolutePath, object : MediaCompressionListener {
                                    override fun onProgress(percent: Float) {
                                        runOnUiThread {}
                                    }

                                    override fun onStart() {
                                        showLoading(true)
                                    }

                                    override fun onSuccess() {
                                        hideLoading()
                                        runOnUiThread {
                                            arrayList.add(file.toUri())
                                            picsList.add(
                                                file.absolutePath
                                            )
                                            addStory()
                                        }
                                    }

                                    override fun onFailure(failureMessage: String) {
                                        hideLoading()
                                    }

                                    override fun onCancelled() {
                                        hideLoading()
                                    }

                                }, MediaQuality.VERY_HIGH, isMinBitRateEnabled = false, keepOriginalResolution = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun addStory() {
        showLoading(true)
        val multipartTypedOutput = arrayOfNulls<MultipartBody.Part>(picsList.size)
        for (i in 0 until picsList.size) {
            val file = File(picsList[i])
            if (checkIsImage(this, arrayList[i])) {
                val surveyBody: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                multipartTypedOutput[i] = MultipartBody.Part.createFormData("media[]", file.path, surveyBody)
                type = "image".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                val surveyBody: RequestBody = file.asRequestBody("video/*".toMediaTypeOrNull())
                multipartTypedOutput[i] = MultipartBody.Part.createFormData("media[]", file.path, surveyBody)
                type = "video".toRequestBody("text/plain".toMediaTypeOrNull())
            }
        }
    }

    private fun checkIsImage(context: Context, uri: Uri?): Boolean {
        val contentResolver = context.contentResolver
        val type = contentResolver.getType(uri!!)
        if (type != null) {
            return type.startsWith("image/")
        } else {
            // try to decode as image (bounds only)
            var inputStream: InputStream? = null
            try {
                inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, options)
                    return options.outWidth > 0 && options.outHeight > 0
                }
            } catch (e: IOException) {
                // ignore
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FileUtils.closeQuietly(inputStream)
                }
            }
        }
        // default outcome if image not confirmed
        return false
    }

}


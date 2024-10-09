package com.meetfriend.app.ui.home.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hbb20.CountryCodePicker
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.post.model.CreatePostRequest
import com.meetfriend.app.api.post.model.EditShortRequest
import com.meetfriend.app.api.post.model.HashTagsResponse
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.api.post.model.MediaRequest
import com.meetfriend.app.api.post.model.MediaSize
import com.meetfriend.app.api.post.model.MultipleImageDetails
import com.meetfriend.app.api.post.model.UploadMediaRequest
import com.meetfriend.app.api.post.model.VideoMediaRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityAddNewPostInfoBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getSelectedHashTags
import com.meetfriend.app.newbase.extension.getSelectedTagUserIds
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.Post
import com.meetfriend.app.ui.activities.TagPeopleListActivity
import com.meetfriend.app.ui.camerakit.SnapkitActivity
import com.meetfriend.app.ui.chatRoom.roomview.view.MentionUserAdapter
import com.meetfriend.app.ui.home.ProgressDialogFragment
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewState
import com.meetfriend.app.ui.location.AddLocationActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FunctionsUtils.getImageDimensions
import com.meetfriend.app.utils.ShareHelper
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddNewPostInfoActivity : BasicActivity() {

    companion object {
        const val INTENT_EXTRA_POST_TYPE = "INTENT_EXTRA_POST_TYPE"
        const val POST_TYPE_IMAGE = "POST_TYPE_IMAGE"
        const val POST_TYPE_VIDEO = "POST_TYPE_VIDEO"
        const val POST_TYPE_TEXT = "POST_TYPE_TEXT"
        const val POST_TYPE_POST_VIDEO = "POST_TYPE_POST_VIDEO"
        const val INTENT_SHORTS_DATA = "INTENT_SHORTS_DATA"
        const val INTENT_TAG_NAME = "INTENT_TAG_NAME"
        const val POST_TYPE_VIDEO_URI = "POST_TYPE_VIDEO_URI"
        const val CREATE_POST = "CREATE_POST"

        const val INTENT_EXTRA_IMAGE_PATH_LIST = "INTENT_EXTRA_IMAGE_PATH_LIST"
        const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"

        private const val REQUEST_CODE_TAG_PEOPLE = 10001
        private const val REQUEST_CODE_LOCATION = 10002
        const val REQUEST_CODE_CAMERA = 10003
        const val TAG = "AddNewPostInfoActivity"
        const val LINK_ATTACHMENT_DETAILS = "LinkAttachmentDetails"
        const val LIST_OF_MULTIPLE_MEDIA = "listOfMultipleMedia"
        const val MUSIC_INFO = "MusicInfo"
        const val TIMEOUT = 400L
        const val ZERO_FIVE = 0.5F
        const val NINETY = 90

        fun launchActivity(
            launchActivityData: LaunchActivityData
        ): Intent {
            val intent = Intent(launchActivityData.context, AddNewPostInfoActivity::class.java)
            intent.putExtra(INTENT_EXTRA_POST_TYPE, launchActivityData.postType)
            intent.putStringArrayListExtra(INTENT_EXTRA_IMAGE_PATH_LIST, launchActivityData.imagePathList)
            intent.putExtra(INTENT_EXTRA_VIDEO_PATH, launchActivityData.videoPath)
            intent.putExtra(INTENT_SHORTS_DATA, launchActivityData.shortsInfo)
            intent.putExtra(POST_TYPE_VIDEO_URI, launchActivityData.videoUri)
            intent.putExtra(LINK_ATTACHMENT_DETAILS, launchActivityData.linkAttachmentDetails)
            intent.putParcelableArrayListExtra(LIST_OF_MULTIPLE_MEDIA, launchActivityData.listOfMultipleMedia)
            intent.putExtra(MUSIC_INFO, launchActivityData.musicResponse)
            if (!launchActivityData.tagName.isNullOrEmpty()) {
                intent.putExtra(
                    INTENT_TAG_NAME,
                    launchActivityData.tagName
                )
            }
            return intent
        }
    }

    private var videoList: List<MultipleImageDetails> = arrayListOf()
    private var imageList: List<MultipleImageDetails> = arrayListOf()
    private var optionIsSelect: String? = "Followers"
    private var listData: java.util.HashMap<Int, Pair<String?, String?>>? = null
    lateinit var binding: ActivityAddNewPostInfoBinding
    private val listOfUploadMediaRequest = arrayListOf<UploadMediaRequest>()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var addNewPostViewModel: AddNewPostViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var mentionUserAdapter: MentionUserAdapter

    private var cloudFlareConfig: CloudFlareConfig? = null

    private var postType = ""
    private var imagePathList = ArrayList<String>()
    private var videoPath = ""
    private var videoId = ""
    private var thumbnail: String? = null
    private var taggedPeopleHashMap = HashMap<Int, String?>()
    private var postPrivacySpinnerValue = 1
    private var imageCount = 0
    private var scrollCount = 0
    private var currentLocation: String? = null
    private var selectedTagUserInfo: MutableList<MeetFriendUser> = mutableListOf()
    private var selectedHashTagUserInfo: MutableList<HashTagsResponse> = mutableListOf()
    private var shortsInfo: Post? = null
    private var width: Int = 0
    private var height: Int = 0
    private var isDialogOpen: Boolean = true
    private var totalRequests: Int = 0
    private var currentRequestIndex: Int = 0
    private var isCancel: Boolean = true
    private var isImageListUpdated: Boolean = true
    private var isRequestDiff: Boolean = false
    private var listOfMultipleMedia: ArrayList<MultipleImageDetails>? = arrayListOf()
    private var listOfMediaSize: ArrayList<MediaSize>? = arrayListOf()
    private var musicResponse: MusicInfo? = null

    private var linkAttachmentDetails: LinkAttachmentDetails? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewPostInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        addNewPostViewModel = getViewModelFromFactory(viewModelFactory)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        listenToViewEvent()
        listenToViewModel()
        if (isOnline(this)) {
            addNewPostViewModel.getCloudFlareConfig(false)
        } else {
            showToast("No internet connection")
        }

        setDataUserData()
        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_TAG_NAME)) {
                it.getStringExtra(INTENT_TAG_NAME)?.let { tagName ->
                    val etCaption = String.format(Locale.getDefault(), "#")
                    binding.etCaption.setText(etCaption.plus(tagName.replace("#", "")))
                }
            }
            binding.tvAddMedia.text = String.format(Locale.getDefault(), "Add Media")

            if (it.hasExtra(INTENT_EXTRA_POST_TYPE)) {
                updateSelectImageUI()

                postType = it.getStringExtra(INTENT_EXTRA_POST_TYPE) ?: ""
                shortsInfo = it.getParcelableExtra(INTENT_SHORTS_DATA)

                linkAttachmentDetails = intent.getParcelableExtra(LINK_ATTACHMENT_DETAILS)

                musicResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(MUSIC_INFO, MusicInfo::class.java)
                } else {
                    intent?.getParcelableExtra(MUSIC_INFO)
                }
                listOfMultipleMedia = intent.getParcelableArrayListExtra(LIST_OF_MULTIPLE_MEDIA) ?: arrayListOf()
                if (shortsInfo != null) {
                    binding.tvHeader.text = resources.getString(R.string.edit_short)
                    if (!shortsInfo?.content.isNullOrEmpty()) {
                        binding.etCaption.setText(shortsInfo?.content.toString())
                    }
                    if (!shortsInfo?.post_media.isNullOrEmpty()) {
                        updateSelectImageUI()
                        Glide.with(this)
                            .load(shortsInfo?.post_media?.first()?.thumbnail)
                            .placeholder(R.drawable.ic_empty_profile_placeholder)
                            .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivSelectedMedia)
                    } else {
                        updateUnSelectImageUI()
                        Glide.with(this)
                            .load(R.drawable.img_create_post)
                            .placeholder(R.drawable.ic_empty_profile_placeholder)
                            .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivSelectedMedia)
                    }

                    if (!shortsInfo?.tagged_users_list.isNullOrEmpty()) {
                        selectedTagUserInfo.addAll(shortsInfo?.mention_users as MutableList<MeetFriendUser>)
                    }

                    binding.locationRelativeLayout.isVisible = false
                    binding.rlTagPeople.isVisible = false

                    listenToViewEvent()
                    listenToViewModel()
                } else {
                    binding.locationRelativeLayout.isVisible = true
                    binding.rlTagPeople.isVisible = true
                    binding.postPrivacySpinners.isVisible = true

                    if (postType.isNotEmpty()) {
                        when (postType) {
                            POST_TYPE_IMAGE -> {
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)

                                val imagePath = it.getStringArrayListExtra(INTENT_EXTRA_IMAGE_PATH_LIST)
                                if (!imagePath.isNullOrEmpty()) {
                                    this.imagePathList = imagePath
                                    getCloudConfig()
                                    getImageSize()
                                } else {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            }

                            POST_TYPE_VIDEO -> {
                                binding.tvHeader.text = resources.getString(R.string.label_create_shorts)

                                val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                                val videoUri = it.getParcelableExtra<Uri>(POST_TYPE_VIDEO_URI) ?: return
                                generateVideoThumbnail(videoUri)
                                if (!videoPath.isNullOrEmpty()) {
                                    this.videoPath = videoPath
                                    getCloudConfig()
                                } else {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            }

                            POST_TYPE_POST_VIDEO -> {
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)

                                val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                                val videoUri = it.getParcelableExtra<Uri>(POST_TYPE_VIDEO_URI) ?: return
                                generateVideoThumbnail(videoUri)
                                if (!videoPath.isNullOrEmpty()) {
                                    this.videoPath = videoPath
                                    getCloudConfig()
                                } else {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            }

                            POST_TYPE_TEXT -> {
                                binding.flSelectedMediaContainer.visibility = View.GONE
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)

                                listenToViewEvent()
                                listenToViewModel()
                            }

                            CREATE_POST -> {
                                updateUnSelectImageUI()
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)
                                binding.tvDone.alpha = ZERO_FIVE
                                binding.tvDone.isEnabled = false
                                binding.tvDone.isClickable = false
                                Glide.with(this)
                                    .load(R.drawable.img_create_post)
                                    .placeholder(R.drawable.img_create_post)
                                    .error(R.drawable.img_create_post).into(binding.ivSelectedMedia)
                            }

                            else -> {
                                onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun updateUnSelectImageUI() {
        binding.ivSelectedMedia.layoutParams.height = resources.getDimensionPixelSize(R.dimen._60sdp)
        binding.ivSelectedMedia.scaleType = ImageView.ScaleType.CENTER_INSIDE
        binding.flSelectedMediaContainer.layoutParams.height = resources.getDimensionPixelSize(R.dimen._150sdp)
        binding.tvAddMedia.visibility = View.VISIBLE
        binding.flSelectedMediaContainer.visibility = View.VISIBLE
    }

    private fun generateVideoThumbnail(videoUri: Uri) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val bitmapImg = retriever.getFrameAtTime(1) // Get the thumbnail at 1st second
        val imgUri = bitmapToUri(bitmapImg)
        getImageDimensions(this@AddNewPostInfoActivity, imgUri.toString()) { width, height ->
            if (width != -1 && height != -1) {
                this.width = width
                this.height = height
            } else {
                Toast.makeText(
                    this@AddNewPostInfoActivity,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        retriever.release()
    }

    private fun bitmapToUri(bitmap: Bitmap?): Uri? {
        // Create a file in the external storage directory
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "thumbnail.jpg")
        try {
            // Open an output stream and save the bitmap to the file
            val out = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, NINETY, out)
            out.flush()
            out.close()
            return Uri.fromFile(file)
        } catch (e: IOException) {
            Timber.tag("AddNewPostInfo").e("Error: $e")
        }
        return null
    }

    private fun getCloudConfig() {
        if (postType == POST_TYPE_IMAGE) {
            if (listOfMultipleMedia?.isNotEmpty() == true) {
                totalRequests = listOfMultipleMedia?.size ?: 0
                val imagePath = listOfMultipleMedia?.firstOrNull()
                    ?.editedImagePath ?: listOfMultipleMedia?.firstOrNull()?.mainImagePath ?: ""
                val imageUri = Uri.fromFile(File(imagePath))
                getImageDimensions(
                    this@AddNewPostInfoActivity,
                    imageUri.toString()
                ) { width, height ->
                    if (width != -1 && height != -1) {
                        this.width = width
                        this.height = height
                    } else {
                        Toast.makeText(
                            this@AddNewPostInfoActivity,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                getImageSize()
                if (listOfMultipleMedia?.firstOrNull()?.isMusicVideo == true) {
                    val videoPath = listOfMultipleMedia?.firstOrNull()?.musicVideoPath
                    Glide.with(
                        this
                    ).load(videoPath).placeholder(R.drawable.img_create_post).into(binding.ivSelectedMedia)
                } else {
                    Glide.with(
                        this
                    ).load(
                        imagePath
                    ).placeholder(R.drawable.img_create_post).listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            finish()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.tvDone.alpha = 1F
                            binding.tvDone.isEnabled = true
                            binding.tvDone.isClickable = true
                            return false
                        }
                    }).into(binding.ivSelectedMedia)
                }

                if ((listOfMultipleMedia?.size ?: 0) > 1) {
                    binding.cvImageCount.visibility = View.VISIBLE
                    binding.cvDelete.visibility = View.VISIBLE
                    imageCount = (listOfMultipleMedia?.size ?: 0) - 1
                    binding.tvImageCount.text = getString(R.string.label_plus_sign)
                        .plus(imageCount).plus(" ").plus(getString(R.string.title_more))
                } else {
                    binding.cvImageCount.visibility = View.GONE
                    binding.cvDelete.visibility = View.GONE
                }
            }
        } else if (postType == POST_TYPE_VIDEO || postType == POST_TYPE_POST_VIDEO) {
            handleVideoPost()
        }
    }
    private fun handleVideoPost() {
        Glide.with(this).load(videoPath).placeholder(R.drawable.img_create_post).into(binding.ivSelectedMedia)

        if (videoPath.isNotEmpty()) {
            binding.tvDone.alpha = 1F
            binding.tvDone.isEnabled = true
            binding.tvDone.isClickable = true
        }
    }

    private fun listenToViewEvent() {
        binding.postPrivacySpinners.isCursorVisible = false
        binding.postPrivacySpinners.inputType = InputType.TYPE_NULL

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.rlTagPeople.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResultWithDefaultAnimation(
                TagPeopleListActivity.launchActivity(this, taggedPeopleHashMap),
                REQUEST_CODE_TAG_PEOPLE
            )
            scrollCount = 0
        }.autoDispose()

        binding.rlWhoCanWatch.throttleClicks().subscribeAndObserveOnMainThread {
            val isPrivate = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isPrivate
            val bottomsheet = WhoCanWatchBottomsheet(isPrivate, listData, optionIsSelect)
            bottomsheet.selectState.subscribeAndObserveOnMainThread {
                optionIsSelect = it.selectedOption
                when (it.selectedOption) {
                    "Followers" -> {
                        if (isPrivate == 1) {
                            binding.tvWhoCanWatch.text = resources.getText(R.string.followers)
                        } else {
                            binding.tvWhoCanWatch.text = resources.getText(R.string.everyone)
                        }
                    }

                    "List" -> {
                        listData = it.list
                        binding.tvWhoCanWatch.text = it.list?.values
                            ?.map { item -> item.first }?.joinToString(separator = ", ")
                    }
                }
            }.autoDispose()

            if (!supportFragmentManager.isStateSaved) {
                bottomsheet.show(supportFragmentManager, AddNewPostInfoActivity::class.java.name)
            }
        }

        binding.tvDone.throttleClicks().subscribeAndObserveOnMainThread {
            if (shortsInfo != null) {
                shortsInfo?.let {
                    addNewPostViewModel.editShort(
                        EditShortRequest(
                            it.id,
                            binding.etCaption.text.toString(),
                            tagNames = getSelectedHashTags(binding.etCaption.toString()),
                            mentionIds = getSelectedTagUserIds(
                                selectedTagUserInfo,
                                binding.etCaption.toString()
                            ),
                            privacy = postPrivacySpinnerValue

                        )
                    )
                }
            } else {
                if (postType == POST_TYPE_TEXT || postType == CREATE_POST) {
                    if (!binding.etCaption.text.isNullOrEmpty()) {
                        createPost()
                    } else {
                        showToast("Please Enter Caption")
                    }
                } else {
                    if (!isCancel) {
                        isCancel = true
                        isDialogOpen = true
                        isImageListUpdated = true
                        buttonVisibility(true)
                    }
                    if (postType == POST_TYPE_IMAGE) {
                        cloudFlareConfig?.let {
                            uploadMediaToCloudFlare(it)
                        } ?: if (isOnline(this)) {
                            addNewPostViewModel.getCloudFlareConfig(true)
                        } else {
                            showToast("No internet connection")
                        }
                    } else if (postType == POST_TYPE_VIDEO || postType == POST_TYPE_POST_VIDEO) {
                        if (isOnline(this)) {
                            cloudFlareConfig?.let {
                                addNewPostViewModel.uploadVideoToCloudFlare(
                                    this@AddNewPostInfoActivity,
                                    it,
                                    File(videoPath),
                                    null
                                )

                                val progressDialogFragment = ProgressDialogFragment.newInstance(postType)
                                progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                                    progressDialogFragment.dismiss()
                                    buttonVisibility(true)
                                }

                                progressDialogFragment.progressCancelState.subscribeAndObserveOnMainThread { item ->
                                    isCancel = item
                                    buttonVisibility(false)
                                    isImageListUpdated = false
                                    currentRequestIndex = 0
                                    listOfUploadMediaRequest.clear()
                                    progressDialogFragment.dismiss()
                                }
                                if (!supportFragmentManager.isStateSaved) {
                                    progressDialogFragment.show(
                                        supportFragmentManager,
                                        ProgressDialogFragment::class.java.name
                                    )
                                }
                            }
                        } else {
                            showToast("No internet connection")
                        }
                    } else {
                        cloudFlareConfig?.let {
                            uploadMediaToCloudFlare(it)
                        } ?: if (isOnline(this)) {
                            addNewPostViewModel.getCloudFlareConfig(true)
                        } else {
                            showToast("No internet connection")
                        }
                    }
                }
            }

            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.postPrivacySpinners.throttleClicks().subscribeAndObserveOnMainThread {
            val arrayList = arrayListOf("Public", "Following")
            val arrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arrayList
            )
            binding.postPrivacySpinners.setAdapter(arrayAdapter)
            binding.postPrivacySpinners.showDropDown()
            binding.postPrivacySpinners.isSelected = true
        }

        binding.postPrivacySpinners.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            binding.postPrivacySpinners.isSelected = true
            val selectedOptionName = parent.getItemAtPosition(position).toString()
            val drawableRight = ContextCompat.getDrawable(this, R.drawable.ic_new_drop_down)
            when (selectedOptionName) {
                resources.getString(R.string.label_public) -> {
                    val drawable = ContextCompat.getDrawable(this, R.drawable.ic_public)
                    binding.postPrivacySpinners.setCompoundDrawablesWithIntrinsicBounds(
                        drawable, null, drawableRight, null
                    )
                    postPrivacySpinnerValue = 1
                }

                resources.getString(R.string.following) -> {
                    val drawable = ContextCompat.getDrawable(this, R.drawable.ic_friend_icon)
                    binding.postPrivacySpinners.setCompoundDrawablesWithIntrinsicBounds(
                        drawable, null, drawableRight, null
                    )
                    postPrivacySpinnerValue = 2
                }
            }
        }

        binding.locationRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResult(AddLocationActivity.getIntent(this), REQUEST_CODE_LOCATION)
            scrollCount = 0
        }.autoDispose()

        binding.cvDelete.throttleClicks().subscribeAndObserveOnMainThread {
            manageDeleteSelectedImage()
        }.autoDispose()

        mentionUserAdapter = MentionUserAdapter(this).apply {
            userClicks.subscribeAndObserveOnMainThread { mentionUser ->
                val cursorPosition: Int = binding.etCaption.selectionStart
                val descriptionString = binding.etCaption.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                addNewPostViewModel.searchUserClicked(
                    binding.etCaption.text.toString(), subString, mentionUser
                )
                if (mentionUser !in selectedTagUserInfo) {
                    selectedTagUserInfo.add(mentionUser)
                }
            }.autoDispose()

            hashTagClicks.subscribeAndObserveOnMainThread { mentionUser ->
                val cursorPosition: Int = binding.etCaption.selectionStart
                val descriptionString = binding.etCaption.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                addNewPostViewModel.searchHashTagClicked(
                    binding.etCaption.text.toString(), subString, mentionUser
                )
                if (mentionUser !in selectedHashTagUserInfo) {
                    selectedHashTagUserInfo.add(mentionUser)
                }
            }.autoDispose()
        }

        binding.rvMentionUserList.apply {
            layoutManager = LinearLayoutManager(
                this@AddNewPostInfoActivity, LinearLayoutManager.VERTICAL, false
            )
            adapter = mentionUserAdapter
        }

        binding.etCaption.textChanges().debounce(
            TIMEOUT,
            TimeUnit.MILLISECONDS,
            Schedulers.io()
        ).subscribeAndObserveOnMainThread {
            if (videoPath.isEmpty()) {
                if (it.isEmpty()) {
//                    binding.tvDone.alpha = 0.5F
//                    binding.tvDone.isEnabled = false
//                    binding.tvDone.isClickable = false
                    binding.llMentionUserListContainer.visibility = View.GONE
                    scrollCount = 0
                } else {
                    binding.tvDone.alpha = 1F
                    binding.tvDone.isEnabled = true
                    binding.tvDone.isClickable = true
                    val lastChar = it.last().toString()
                    if (!lastChar.contains("@")) {
                        val wordList = it.split(" ")
                        val lastWord = wordList.last()
                        val search: String = lastWord.substringAfterLast("@")
                        val searchHash: String = lastWord.substringAfterLast("#")

                        if (lastWord.contains("@")) {
                            addNewPostViewModel.getUserForMention(search)
                        } else if (lastWord.contains("#")) {
                            addNewPostViewModel.getHashTagList(searchHash)
                        } else {
                            binding.llMentionUserListContainer.visibility = View.GONE
                        }
                    }
                }
            }
        }.autoDispose()

        binding.ivSelectedMedia.setOnClickListener {
            if (imagePathList.isEmpty() && videoPath.isEmpty()) {
                val intent = SnapkitActivity.getIntent(this@AddNewPostInfoActivity, false)
                startActivityForResult(intent, REQUEST_CODE_CAMERA)
            }
        }
    }

    private fun listenToViewModel() {
        addNewPostViewModel.addNewPostState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewPostViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }

                is AddNewPostViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressedDispatcher.onBackPressed()
                }

                is AddNewPostViewState.UploadImageCloudFlareSuccess -> {
                    if (isImageListUpdated) {
                        isRequestDiff = currentRequestIndex == it.currentRequestIndex
                        if (currentRequestIndex == it.currentRequestIndex) {
                            currentRequestIndex++
                            listOfUploadMediaRequest.add(UploadMediaRequest(isVideo = false, imageUrl = it.imageUrl))
                            cloudFlareConfig?.let { cConfig ->
                                // Ensure any UI updates or fragment transactions are done safely
                                Handler(Looper.getMainLooper()).post {
                                    uploadMediaToCloudFlare(cConfig)
                                }
                            } ?: run {
                                Handler(Looper.getMainLooper()).post {
                                    addNewPostViewModel.getCloudFlareConfig(false)
                                }
                            }
                        } else {
                            buttonVisibility(true)
                            cloudFlareConfig = null
                            currentRequestIndex = 0
                            listOfUploadMediaRequest.clear()
                            cloudFlareConfig?.let { cConfig ->
                                uploadMediaToCloudFlare(cConfig)
                            } ?: addNewPostViewModel.getCloudFlareConfig(true)
                        }
                    }
                }

                is AddNewPostViewState.UploadVideoCloudFlareSuccess -> {
                    if (it.currentRequestIndex == null) {
                        if (isImageListUpdated) {
                            videoId = it.videoId
                            thumbnail = it.thumbnail
                            createPost()
                        }
                    } else {
                        if (isImageListUpdated) {
                            isRequestDiff = currentRequestIndex == it.currentRequestIndex
                            if (currentRequestIndex == it.currentRequestIndex) {
                                currentRequestIndex++
                                listOfUploadMediaRequest.add(
                                    UploadMediaRequest(isVideo = true, videoId = it.videoId, thumbnail = it.thumbnail)
                                )
                                cloudFlareConfig?.let { cConfig ->
                                    uploadMediaToCloudFlare(cConfig)
                                } ?: addNewPostViewModel.getCloudFlareConfig(false)
                            } else {
                                buttonVisibility(true)
                                cloudFlareConfig = null
                                currentRequestIndex = 0
                                listOfUploadMediaRequest.clear()
                                cloudFlareConfig?.let { cConfig ->
                                    uploadMediaToCloudFlare(cConfig)
                                } ?: addNewPostViewModel.getCloudFlareConfig(true)
                            }
                        }
                    }
                }

                is AddNewPostViewState.CreatePostSuccessMessage -> {
                    currentRequestIndex = 0
                    cloudFlareConfig = null
                    isImageListUpdated = false
                    isCancel = false
                    if (!intent.hasExtra(INTENT_TAG_NAME)) {
                        startActivity(MainHomeActivity.getIntent(this))
                    }
                    finish()
                }

                is AddNewPostViewState.CreateShortSuccessMessage -> {
                    currentRequestIndex = 0
                    cloudFlareConfig = null
                    isImageListUpdated = false
                    isCancel = false
                    startActivity(MainHomeActivity.getIntentFrom(this, 1))
                    finish()
                }

                is AddNewPostViewState.CreatePostType -> {
                    val props = JSONObject()
                    if (postType == POST_TYPE_VIDEO) props.put("content_type", "shorts")

                    if (postType == POST_TYPE_POST_VIDEO) props.put("content_type", "Video Post")

                    if (postType == POST_TYPE_IMAGE) props.put("content_type", "Image Post")

                    if (postType == POST_TYPE_TEXT) props.put("content_type", "Text Post")

                    val type = if (postType == POST_TYPE_VIDEO) "shorts count" else "post count"
                    mp?.people?.increment(type, 1.0)
                    it.postId?.let { postId ->
                        ShareHelper.shareDeepLink(
                            this,
                            if (postType == POST_TYPE_VIDEO) 1 else 0,
                            postId,
                            "",
                            false
                        ) { link ->
                            props.put("link", link)
                            mp?.track("Create Content", props)
                        }
                    }
                }

                is AddNewPostViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }

                is AddNewPostViewState.LoadingState -> {
                }

                is AddNewPostViewState.UserListForMention -> {
                    mentionUserViewVisibility(!it.listOfUserForMention.isNullOrEmpty())
                    mentionUserAdapter.listOfDataItems = it.listOfUserForMention
                    mentionUserAdapter.listOfHashTags = null
                }

                is AddNewPostViewState.HashTagListForMention -> {
                    mentionUserViewVisibility(!it.listOfUserForMention.isNullOrEmpty())
                    mentionUserAdapter.listOfHashTags = it.listOfUserForMention
                    mentionUserAdapter.listOfDataItems = null
                }

                is AddNewPostViewState.UpdateDescriptionText -> {
                    mentionUserViewVisibility(false)
                    binding.etCaption.setText(it.descriptionString)
                    binding.etCaption.setSelection(binding.etCaption.text.toString().length)
                }

                is AddNewPostViewState.EditShortSuccessMessage -> {
                    showToast("short updated successfully")
                    RxBus.publish(RxEvent.ShortsEditedSuccessfully)
                    finish()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun mentionUserViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.llMentionUserListContainer.visibility == View.GONE) {
            binding.llMentionUserListContainer.visibility = View.VISIBLE
            if (scrollCount == 0) {
                binding.nestedScrollView.smoothScrollTo(0, binding.flSelectedMediaContainer.bottom)
                scrollCount++
            }
        } else if (!isVisibility && binding.llMentionUserListContainer.visibility == View.VISIBLE) {
            binding.llMentionUserListContainer.visibility = View.GONE
            scrollCount = 0
        }
    }

    private fun uploadMediaToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (postType == POST_TYPE_IMAGE && isCancel) {
            if (currentRequestIndex != listOfMultipleMedia?.size) {
                if (isOnline(this)) {
                    setCloudFlareConfig(cloudFlareConfig)
                } else {
                    showToast("No internet connection")
                }
            } else {
                isImageListUpdated = false
                createPost()
            }
        }
    }

    private fun setCloudFlareConfig(cloudFlareConfig: CloudFlareConfig) {
        if (listOfMultipleMedia?.get(currentRequestIndex)?.isMusicVideo == true) {
            val videoPath = listOfMultipleMedia?.get(currentRequestIndex)?.musicVideoPath
            addNewPostViewModel.uploadVideoToCloudFlare(
                this@AddNewPostInfoActivity,
                cloudFlareConfig,
                File(videoPath),
                currentRequestIndex
            )
        } else {
            val imagePath = listOfMultipleMedia?.get(currentRequestIndex)?.editedImagePath
                ?: listOfMultipleMedia?.get(currentRequestIndex)?.mainImagePath ?: ""
            val imageFile = File(imagePath)
            addNewPostViewModel.uploadImageToCloudFlare(
                this,
                cloudFlareConfig,
                imageFile,
                totalRequests,
                currentRequestIndex
            )
        }
        if (isDialogOpen) {
            isDialogOpen = false
            val progressDialogFragment = ProgressDialogFragment.newInstance(postType)
            progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                if (currentRequestIndex == (listOfMultipleMedia?.size?.minus(1))) {
                    progressDialogFragment.dismiss()
                    buttonVisibility(true)
                }
            }
            progressDialogFragment.progressCancelState.subscribeAndObserveOnMainThread {
                isCancel = it
                buttonVisibility(false)
                isImageListUpdated = false
                currentRequestIndex = 0
                listOfUploadMediaRequest.clear()
                progressDialogFragment.dismiss()
            }
            if (!supportFragmentManager.isStateSaved) {
                progressDialogFragment.show(supportFragmentManager, ProgressDialogFragment::class.java.name)
            }
        }
    }

    private fun manageDeleteSelectedImage() {
        if ((listOfMultipleMedia?.size ?: 0) > 1) {
            listOfMultipleMedia?.removeFirst()
            imageCount -= 1
            totalRequests = listOfMultipleMedia?.size ?: 0
            if (imageCount > 0) {
                binding.tvImageCount.text = getString(R.string.label_plus_sign)
                    .plus(imageCount).plus(" ").plus(getString(R.string.title_more))
            }

            val imagePath = listOfMultipleMedia
                ?.firstOrNull()?.editedImagePath ?: listOfMultipleMedia?.firstOrNull()?.mainImagePath ?: ""
            Glide.with(
                this
            ).load(imagePath).placeholder(R.drawable.ic_empty_profile_placeholder).into(binding.ivSelectedMedia)
            // Adjust visibility logic here
            if ((listOfMultipleMedia?.size ?: 0) <= 1) {
                binding.cvDelete.visibility = View.GONE
                binding.cvImageCount.visibility = View.GONE
            } else {
                binding.cvDelete.visibility = View.VISIBLE
                binding.cvImageCount.visibility = View.VISIBLE
            }
        }
    }

    private fun setDataUserData() {
        val userName = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
        binding.tvName.text =
            if (!userName.isNullOrEmpty() && userName != "null") {
                userName
            } else {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(
                    " "
                ).plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName)
            }

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(
            this
        ).load(
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto
        ).placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_TAG_PEOPLE) {
                data?.let {
                    if (it.hasExtra(TagPeopleListActivity.INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)) {
                        val taggedPeopleHashMap = it.getSerializableExtra(
                            TagPeopleListActivity.INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP
                        )
                        if (taggedPeopleHashMap != null) {
                            this.taggedPeopleHashMap = taggedPeopleHashMap as HashMap<Int, String?>
                            val taggedPeopleList = taggedPeopleHashMap.values
                            if (taggedPeopleList.isNotEmpty()) {
                                binding.tvSelectedPeople.text = TextUtils.join(", ", taggedPeopleList)
                            }
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_LOCATION) {
                if (data != null) {
                    currentLocation = data.getStringExtra(AddLocationActivity.LOCATION_ADDRESS)
                    binding.tvSelectedLocation.text = currentLocation
                }
            } else if (requestCode == REQUEST_CODE_CAMERA) {
                data?.let {
                    if (!it.getStringExtra(INTENT_EXTRA_POST_TYPE).isNullOrEmpty()) {
                        postType = it.getStringExtra(INTENT_EXTRA_POST_TYPE) ?: ""
                        when (postType) {
                            POST_TYPE_IMAGE -> {
                                linkAttachmentDetails = it.getParcelableExtra(LINK_ATTACHMENT_DETAILS)
                                listOfMultipleMedia = it.getParcelableArrayListExtra(
                                    LIST_OF_MULTIPLE_MEDIA
                                ) ?: arrayListOf()
                                updateSelectImageUI()
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)

                                videoList = listOfMultipleMedia?.filter {
                                        item ->
                                    item.isMusicVideo == true
                                } ?: arrayListOf()
                                imageList = listOfMultipleMedia?.filter {
                                        item ->
                                    item.isMusicVideo == false
                                } ?: arrayListOf()
                                if (videoList.isNotEmpty() && imageList.isNotEmpty()) {
                                    listOfMultipleMedia?.clear()
                                    listOfMultipleMedia?.addAll(imageList)
                                    listOfMultipleMedia?.addAll(videoList)
                                } else {
                                    if (imageList.isNotEmpty()) {
                                        listOfMultipleMedia?.clear()
                                        listOfMultipleMedia?.addAll(imageList)
                                    } else if (videoList.isNotEmpty()) {
                                        listOfMultipleMedia?.clear()
                                        listOfMultipleMedia?.addAll(videoList)
                                    } else {
                                        listOfMultipleMedia?.clear()
                                        listOfMultipleMedia?.addAll(imageList)
                                        listOfMultipleMedia?.addAll(videoList)
                                    }
                                }

                                if (!listOfMultipleMedia.isNullOrEmpty()) {
                                    getCloudConfig()
                                } else {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            }

                            POST_TYPE_POST_VIDEO -> {
                                linkAttachmentDetails = it.getParcelableExtra(LINK_ATTACHMENT_DETAILS)
                                listOfMultipleMedia = it.getParcelableArrayListExtra(
                                    LIST_OF_MULTIPLE_MEDIA
                                ) ?: arrayListOf()
                                updateSelectImageUI()
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)

                                val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                                val videoUri = it.getParcelableExtra<Uri>(POST_TYPE_VIDEO_URI) ?: return

                                musicResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    it.getParcelableExtra(MUSIC_INFO, MusicInfo::class.java)
                                } else {
                                    it.getParcelableExtra(MUSIC_INFO)
                                }
                                generateVideoThumbnail(videoUri)
                                if (!videoPath.isNullOrEmpty()) {
                                    this.videoPath = videoPath
                                    getCloudConfig()
                                } else {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            }

                            POST_TYPE_TEXT -> {
                                binding.flSelectedMediaContainer.visibility = View.GONE
                                binding.tvHeader.text = resources.getString(R.string.label_create_post)

                                listenToViewEvent()
                                listenToViewModel()
                            }

                            else -> {
                                onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }
    }

    private fun updateSelectImageUI() {
        binding.tvAddMedia.visibility = View.GONE
        val heightInPixels = resources.getDimensionPixelSize(R.dimen._250sdp)
        binding.ivSelectedMedia.layoutParams.height = heightInPixels
        binding.flSelectedMediaContainer.layoutParams.height = heightInPixels
        binding.ivSelectedMedia.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun createPost() {
        if (isOnline(this)) {
            val request = CreatePostRequest()
            request.content = binding.etCaption.text.toString()

            val taggedPeopleList = taggedPeopleHashMap.keys
            request.taggedUserId = if (taggedPeopleList.isNotEmpty()) {
                TextUtils.join(",", taggedPeopleList)
            } else {
                ""
            }

            val allowUsers = listData?.keys?.toMutableList()
            request.allowedUser = if (allowUsers?.isNotEmpty() == true) {
                allowUsers
            } else {
                emptyList()
            }
            val thumbnailList: MutableList<String> = mutableListOf()
            val videoIdList: MutableList<VideoMediaRequest> = mutableListOf()

            if (postType == POST_TYPE_VIDEO || postType == POST_TYPE_POST_VIDEO) {
                if (musicResponse != null) {
                    if (linkAttachmentDetails != null && !linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                        linkAttachmentDetails?.let {
                            val listOfPositionArray = arrayListOf<Float>()
                            listOfPositionArray.add(it.finalX ?: 0.0F)
                            listOfPositionArray.add(it.finalY ?: 0.0F)
                            listOfPositionArray.add(it.lastWidth ?: 0.0F)
                            listOfPositionArray.add(it.lastHeight ?: 0.0F)
                            videoIdList.add(
                                VideoMediaRequest(
                                    uid = videoId,
                                    musicResponse?.name,
                                    getArtistsNames(),
                                    webLink = linkAttachmentDetails?.attachUrl,
                                    position = listOfPositionArray.toString(),
                                    rotationAngle = linkAttachmentDetails?.lastRotation,
                                    width = this.width,
                                    height = this.height
                                )
                            )
                        }
                    } else {
                        videoIdList.add(
                            VideoMediaRequest(
                                uid = videoId,
                                musicResponse?.name,
                                getArtistsNames(),
                                "",
                                0.0f,
                                width = this.width,
                                height = this.height
                            )
                        )
                    }
                } else {
                    if (linkAttachmentDetails != null && !linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                        linkAttachmentDetails?.let {
                            val listOfPositionArray = arrayListOf<Float>()
                            listOfPositionArray.add(it.finalX ?: 0.0F)
                            listOfPositionArray.add(it.finalY ?: 0.0F)
                            listOfPositionArray.add(it.lastWidth ?: 0.0F)
                            listOfPositionArray.add(it.lastHeight ?: 0.0F)
                            videoIdList.add(
                                VideoMediaRequest(
                                    uid = videoId,
                                    null,
                                    null,
                                    webLink = linkAttachmentDetails?.attachUrl,
                                    position = listOfPositionArray.toString(),
                                    rotationAngle = linkAttachmentDetails?.lastRotation,
                                    width = this.width,
                                    height = this.height
                                )
                            )
                        }
                    } else {
                        videoIdList.add(
                            VideoMediaRequest(
                                uid = videoId,
                                null,
                                null,
                                "",
                                0.0f,
                                "",
                                width = this.width,
                                height = this.height
                            )
                        )
                    }
                }
                thumbnailList.add(thumbnail.toString())
                request.videoId = videoIdList
                request.thumbnail = thumbnailList
                request.width = this.width
                request.height = this.height
                request.media = arrayListOf()
            } else if (postType == POST_TYPE_IMAGE) {
                val listOfMediaRequest = arrayListOf<MediaRequest>()
                if (listOfUploadMediaRequest.isNotEmpty()) {
                    listOfUploadMediaRequest.forEachIndexed { index, s ->
                        if (s.isVideo == false) {
                            if (listOfMultipleMedia?.get(index)?.attachUrl.isNullOrEmpty()) {
                                val mediaRequest = MediaRequest()
                                mediaRequest.image = s.imageUrl
                                mediaRequest.rotationAngle = 0f
                                mediaRequest.position = ""
                                mediaRequest.webLink = ""
                                mediaRequest.width = listOfMediaSize?.get(index)?.width
                                mediaRequest.height = listOfMediaSize?.get(index)?.height
                                listOfMediaRequest.add(mediaRequest)
                            } else {
                                listOfMultipleMedia?.get(index)?.attachUrl?.let {
                                    val mediaRequest = MediaRequest()
                                    mediaRequest.height = 0
                                    mediaRequest.width = 0
                                    mediaRequest.webLink = it
                                    val listOfPositionArray = arrayListOf<Float>()
                                    listOfPositionArray.add(listOfMultipleMedia?.get(index)?.finalX ?: 0.0F)
                                    listOfPositionArray.add(listOfMultipleMedia?.get(index)?.finalY ?: 0.0F)
                                    listOfPositionArray.add(listOfMultipleMedia?.get(index)?.lastWidth ?: 0.0F)
                                    listOfPositionArray.add(listOfMultipleMedia?.get(index)?.lastHeight ?: 0.0F)
                                    mediaRequest.position = listOfPositionArray.toString()
                                    mediaRequest.image = s.imageUrl
                                    mediaRequest.width = listOfMediaSize?.get(index)?.width
                                    mediaRequest.height = listOfMediaSize?.get(index)?.height
                                    mediaRequest.rotationAngle = listOfMultipleMedia?.get(index)?.lastRotation
                                    listOfMediaRequest.add(mediaRequest)
                                }
                            }
                        } else {
                            if (listOfMultipleMedia?.get(index)?.musicInfo != null) {
                                val artistsName =
                                    listOfMultipleMedia?.get(
                                        index
                                    )?.musicInfo?.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
                                if (listOfMultipleMedia?.get(index)?.attachUrl.isNullOrEmpty()) {
                                    videoIdList.add(
                                        VideoMediaRequest(
                                            uid = s.videoId,
                                            listOfMultipleMedia?.get(index)?.musicInfo?.name,
                                            artistsName,
                                            height = listOfMultipleMedia?.get(index)?.height,
                                            width = listOfMultipleMedia?.get(index)?.width
                                        )
                                    )
                                } else {
                                    listOfMultipleMedia?.get(index)?.attachUrl?.let {
                                        val listOfPositionArray = arrayListOf<Float>()
                                        val rotation = listOfMultipleMedia?.get(index)?.lastRotation
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.finalX ?: 0.0F)
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.finalY ?: 0.0F)
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.lastWidth ?: 0.0F)
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.lastHeight ?: 0.0F)
                                        videoIdList.add(
                                            VideoMediaRequest(
                                                uid = s.videoId,
                                                listOfMultipleMedia?.get(index)?.musicInfo?.name,
                                                artistsName,
                                                webLink = it,
                                                position = listOfPositionArray.toString(),
                                                rotationAngle = rotation,
                                                height = listOfMultipleMedia?.get(index)?.height,
                                                width = listOfMultipleMedia?.get(index)?.width
                                            )
                                        )
                                    }
                                }
                            } else {
                                val artistsName =
                                    listOfMultipleMedia?.get(
                                        index
                                    )?.musicInfo?.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
                                if (listOfMultipleMedia?.get(index)?.attachUrl.isNullOrEmpty()) {
                                    videoIdList.add(
                                        VideoMediaRequest(
                                            uid = s.videoId,
                                            listOfMultipleMedia?.get(index)?.musicInfo?.name,
                                            artistsName,
                                            height = listOfMultipleMedia?.get(index)?.height,
                                            width = listOfMultipleMedia?.get(index)?.width
                                        )
                                    )
                                } else {
                                    listOfMultipleMedia?.get(index)?.attachUrl?.let {
                                        val listOfPositionArray = arrayListOf<Float>()
                                        val rotation = listOfMultipleMedia?.get(index)?.lastRotation
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.finalX ?: 0.0F)
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.finalY ?: 0.0F)
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.lastWidth ?: 0.0F)
                                        listOfPositionArray.add(listOfMultipleMedia?.get(index)?.lastHeight ?: 0.0F)
                                        videoIdList.add(
                                            VideoMediaRequest(
                                                uid = s.videoId,
                                                listOfMultipleMedia?.get(index)?.musicInfo?.name,
                                                artistsName,
                                                webLink = it,
                                                position = listOfPositionArray.toString(),
                                                rotationAngle = rotation,
                                                height = listOfMultipleMedia?.get(index)?.height,
                                                width = listOfMultipleMedia?.get(index)?.width
                                            )
                                        )
                                    }
                                }
                                videoIdList.add(VideoMediaRequest(uid = s.videoId, null, null))
                            }
                        }
                    }
                    if (videoIdList.isNotEmpty()) {
                        request.videoId = videoIdList
                    }
                    request.media = listOfMediaRequest
                } else {
                    request.media = arrayListOf()
                }
            }

            request.platform = "Android"
            if (!currentLocation.isNullOrEmpty()) {
                request.location = currentLocation
            } else {
                request.location = ""
            }
            request.typeOfMedia = if (postType == POST_TYPE_VIDEO) "shorts" else "post"
            request.privacy = postPrivacySpinnerValue
            request.mentionIds = getSelectedTagUserIds(selectedTagUserInfo, binding.etCaption.toString()) ?: ""
            request.tagNames = getSelectedHashTags(binding.etCaption.toString())
            setlinkAttachmentDetails(request)

            val customView = LayoutInflater.from(this@AddNewPostInfoActivity).inflate(R.layout.custom_tab_layout, null)
            val ccp: CountryCodePicker = customView.findViewById(R.id.ccp)
            ccp.setDetectCountryWithAreaCode(true)
            ccp.setAutoDetectedCountry(true)
            request.countryCode = ccp.defaultCountryNameCode
            addNewPostViewModel.createPost(request, postType)
        } else {
            showToast("No internet connection")
        }
    }
    private fun setlinkAttachmentDetails(request: CreatePostRequest) {
        if (linkAttachmentDetails != null && !linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
            linkAttachmentDetails?.let {
                val listOfPositionArray = arrayListOf<Float>()
                listOfPositionArray.add(it.finalX ?: 0.0F)
                listOfPositionArray.add(it.finalY ?: 0.0F)
                listOfPositionArray.add(it.lastWidth ?: 0.0F)
                listOfPositionArray.add(it.lastHeight ?: 0.0F)
                request.rotationAngle = it.lastRotation?.toDouble()
                request.webLink = it.attachUrl
                request.position = listOfPositionArray.toString()
            }
        } else {
            request.webLink = ""
            request.rotationAngle = 0.0
            request.position = ""
        }
    }
    private fun getArtistsNames(): String? {
        return musicResponse?.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
    }

    private fun getImageSize() {
        listOfMultipleMedia?.forEach {
            val s = it.editedImagePath ?: it.mainImagePath ?: ""
            val imageUri = Uri.fromFile(File(s))
            getImageDimensions(
                this@AddNewPostInfoActivity,
                imageUri.toString()
            ) { width, height ->
                if (width != -1 && height != -1) {
                    listOfMediaSize?.add(MediaSize(width, height))
                } else {
                    Toast.makeText(
                        this@AddNewPostInfoActivity,
                        "Something went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        return capabilities?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }
}

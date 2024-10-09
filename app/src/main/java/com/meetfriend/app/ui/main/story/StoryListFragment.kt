package com.meetfriend.app.ui.main.story

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoSize
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentStoryListBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ViewOneToOneChatViewModel
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewModel
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewState
import com.meetfriend.app.ui.storylibrary.StoriesProgressView
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.ui.storywork.models.StoriesResponse
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utils.Constant
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import okhttp3.RequestBody
import javax.inject.Inject
import kotlin.properties.Delegates

class StoryListFragment : BasicFragment(), StoriesProgressView.StoriesListener {

    private var _binding: FragmentStoryListBinding? = null
    private val binding get() = _binding!!

    lateinit var resources: MutableList<StoriesResponse>
    private var counter = 0
    private var position = 0
    private var savedPosition: Int? = null
    private var storyListResponse: ResultListResult? = null

    @Inject
    internal lateinit var viewModelFactoryChat: ViewModelFactory<ViewOneToOneChatViewModel>
    private lateinit var viewOneToOneChatViewModel: ViewOneToOneChatViewModel

    var size by Delegates.notNull<Int>()
    lateinit var type: RequestBody
    private var visible = false
    private lateinit var stories: StoriesResponse
    private lateinit var videoPlayer: SimpleExoPlayer
    var reportDialog: Dialog? = null
    private var isPlayerInitialized = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MainHomeViewModel>
    private lateinit var mainHomeViewModel: MainHomeViewModel

    companion object {
        var onClick: ((Int) -> Unit?)? = null
        var onBackPress: ((Int) -> Unit?)? = null
        const val POSITION = "POSITION"
        const val STORY_LIST_RESPONSE = "STORY_LIST_RESPONSE"
        const val STORY_DURATION = 1000L
        const val STORY_DURATION_FOR_IMAGE = 7000L
        const val FIX_MARGIN = 40
        const val HUNDRED = 100
        const val THIRD = 3

        @JvmStatic
        fun newInstance(
            onClick: (Int) -> Unit,
            onBackPress: (Int) -> Unit,
            number: Int,
            storyListResponse: ResultListResult
        ): StoryListFragment {
            this.onClick = onClick
            this.onBackPress = onBackPress
            val storyListFragment = StoryListFragment()
            val bundle = Bundle().apply {
                putInt(POSITION, number)
                putParcelable(STORY_LIST_RESPONSE, storyListResponse)
            }
            storyListFragment.arguments = bundle
            return storyListFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MeetFriendApplication.component.inject(this)
        mainHomeViewModel = getViewModelFromFactory(viewModelFactory)
        viewOneToOneChatViewModel = getViewModelFromFactory(viewModelFactoryChat)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        position = arguments?.getInt(POSITION) ?: 0
        storyListResponse = arguments?.getParcelable(STORY_LIST_RESPONSE)
        initPlayer()
        initUIInfo()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        mainHomeViewModel.mainHomeState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is MainHomeViewState.ErrorMessage -> {
                    requireActivity().showToast(state.errorMessage)
                }
                is MainHomeViewState.SuccessMessage -> {
                    handleSuccessMessage(state)
                }
                is MainHomeViewState.GetConversation -> {
                    handleGetConversation(state)
                }
                else -> { /* Handle other states if necessary */ }
            }
        }.autoDispose()
    }

    private fun handleSuccessMessage(state: MainHomeViewState.SuccessMessage) {
        requireActivity().showToast(state.successMessage)
        resources.removeAt(counter)

        if (resources.isNotEmpty()) {
            if (resources.size > counter) {
                setupStories()
            } else {
                onClick?.let { it(position) }
            }
        } else {
            onClick?.let { it(position) }
        }
    }

    private fun setupStories() {
        binding.stories.setStoriesCount(resources.size)
        size = resources.size
        binding.stories.setStoriesListener(this)

        val currentResource = resources[counter]
        if (currentResource.type == "video") {
            val videoDuration = ((stories.duration?.toFloat() ?: 0f) * STORY_DURATION).toLong()
            binding.stories.setStoryDuration(videoDuration)
            currentResource.video_url?.let { startVideo(it) }
        } else {
            binding.stories.setStoryDuration(STORY_DURATION_FOR_IMAGE)
            currentResource.file_path?.let { showImage(it) }
        }

        binding.stories.startStories(counter)
    }

    private fun handleGetConversation(state: MainHomeViewState.GetConversation) {
        val request = createSendPrivateMessageRequest(state.conversationId)
        viewOneToOneChatViewModel.sendPrivateMessage(request)
        binding.edittext.text?.clear()
        requireActivity().hideKeyboard()
    }

    private fun createSendPrivateMessageRequest(conversationId: Int): SendPrivateMessageRequest {
        return SendPrivateMessageRequest(
            conversationId = conversationId,
            senderId = loggedInUserCache.getLoggedInUserId(),
            fileUrl = stories.file_path,
            messageType = Constant.MESSAGE_TYPE_STORY_REPLAY,
            senderName = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName,
            conversationName = "",
            senderProfile = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto,
            receiverId = stories.user_id,
            storyId = stories.id,
            message = binding.edittext.text.toString(),
            roomType = THIRD,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            videoUrl = stories.video_url,
            isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
        )
    }

    private fun initPlayer() {
        try {
            videoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            binding.playerView.player = videoPlayer
            val audioAttributes = AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build()
            videoPlayer.setAudioAttributes(audioAttributes, true)
            isPlayerInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            isPlayerInitialized = false
        }
    }

    private fun initUIInfo() {
        setKeyboardVisibilityListener()
        startStories(storyListResponse?.stories)
        binding.reverse.throttleClicks().subscribeAndObserveOnMainThread {
            if (counter == 0 && position != 0) {
                onBackPress?.let { onBackPress -> onBackPress(position) }
            } else {
                binding.stories.reverse()
            }
        }.autoDispose()

        binding.skip.throttleClicks().subscribeAndObserveOnMainThread {
            binding.stories.skip()
        }.autoDispose()

        binding.rlDeleteStory.throttleClicks().subscribeAndObserveOnMainThread {
            binding.deleteview.visibility = View.GONE
            binding.stories.pauseStory(counter)
            if (this@StoryListFragment::videoPlayer.isInitialized) {
                videoPlayer.stop()
            }
            deleteStory()
        }.autoDispose()

        binding.ivMenu.throttleClicks().subscribeAndObserveOnMainThread {
            performMenuClick()
        }.autoDispose()

        binding.report.setOnClickListener {
            if (this::stories.isInitialized) {
                Handler(Looper.myLooper()!!).post { binding.stories.pause() }
                showReportDialog()
            }
        }

        binding.edittext.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.stories.pause()
            } else {
                binding.stories.resume()
            }
        }
        binding.send.throttleClicks().subscribeAndObserveOnMainThread {
            performSendMessageClick()
        }

        Glide.with(requireContext())
            .load(storyListResponse?.profile_photo)
            .placeholder(R.drawable.place_holder_image)
            .into(binding.ivProfile)

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().finish()
        }.autoDispose()
    }

    private fun performSendMessageClick() {
        if (binding.edittext.text.toString().isNotEmpty()) {
            if (storyListResponse?.conversationId == null || storyListResponse?.conversationId == 0) {
                mainHomeViewModel.getConversationId(stories.user_id)
            } else {
                val request = SendPrivateMessageRequest(
                    conversationId = storyListResponse?.conversationId,
                    senderId = loggedInUserCache.getLoggedInUserId(),
                    fileUrl = stories.file_path,
                    messageType = Constant.MESSAGE_TYPE_STORY_REPLAY,
                    senderName = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName,
                    conversationName = "",
                    senderProfile = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto,
                    receiverId = stories.user_id,
                    storyId = stories.id,
                    message = binding.edittext.text.toString(),
                    roomType = THIRD,
                    gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
                    age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
                    videoUrl = stories.video_url,
                    isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
                )
                showToast("Send")
                viewOneToOneChatViewModel.sendPrivateMessage(request)

                binding.edittext.text?.clear()
                requireActivity().hideKeyboard()
            }
        }
    }

    private fun performMenuClick() {
        if (loggedInUserCache.getLoggedInUserId() == storyListResponse?.stories?.get(0)?.user_id) {
            if (binding.deleteview.visibility == View.GONE) {
                binding.deleteview.visibility = View.VISIBLE
                binding.report.visibility = View.GONE
                binding.replyview.visibility = View.GONE
            } else {
                binding.deleteview.visibility = View.GONE
            }
        } else {
            if (binding.replyview.visibility == View.VISIBLE) {
                binding.deleteview.visibility = View.GONE
                binding.report.visibility = View.VISIBLE
                binding.replyview.visibility = View.GONE
            } else {
                binding.replyview.visibility = View.VISIBLE
                binding.deleteview.visibility = View.GONE
                binding.report.visibility = View.GONE
            }
        }
    }

    private fun deleteStory() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete this story?")
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            val infoId = resources[counter].id.toString()
            mainHomeViewModel.deleteStory(infoId)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
            binding.stories.resumeStory(counter)
            if (this@StoryListFragment::videoPlayer.isInitialized) {
                videoPlayer.play()
            }
        }
        val alert = builder.create()
        alert.show()
    }

    private fun setKeyboardVisibilityListener() {
        setEventListener(
            requireActivity(),
            viewLifecycleOwner,
            KeyboardVisibilityEventListener { isVisible ->
                handleKeyboardVisibilityChange(isVisible)
            }
        )
    }

    private fun handleKeyboardVisibilityChange(isVisible: Boolean) {
        if (isVisible) {
            disableStoryControls()
            if ((storyListResponse?.stories?.get(counter)?.type ?: "video") != "image") {
                videoPlayer.pause()
            }
        } else {
            enableStoryControls()
            if ((storyListResponse?.stories?.get(counter)?.type ?: "video") != "image") {
                videoPlayer.play()
            }
        }
    }

    private fun disableStoryControls() {
        binding.skip.isClickable = false
        binding.reverse.isClickable = false
        binding.stories.pauseStory(counter)
    }

    private fun enableStoryControls() {
        binding.skip.isClickable = true
        binding.reverse.isClickable = true
        binding.stories.resumeStory(counter)
    }

    private fun showReportDialog() {
        binding.report.visibility = View.GONE
        val reportDialog = ReportDialogFragment.newInstance(stories.id, false, Constant.STORY)
        reportDialog.optionClick.subscribeAndObserveOnMainThread {
            if (it == Constant.DISMISS) {
                Handler(Looper.myLooper()!!).post { binding.stories.resume() }
                binding.replyview.visibility = View.VISIBLE
            }
        }.autoDispose()
        reportDialog.show(
            childFragmentManager,
            ReportDialogFragment::class.java.name
        )
    }

    private fun startStories(storyDetailResponse: ArrayList<StoriesResponse>?) {
        if (!storyDetailResponse.isNullOrEmpty()) {
            resources = storyDetailResponse
            size = resources.size
            binding.stories.setStoriesCount(size)
            binding.stories.setStoriesListener(this)

            // Find the index of the first story with isSelected = true
            val selectedStoryIndex = resources.indexOfFirst { it.isSelected }

            counter = if (savedPosition != null) {
                savedPosition!!
            } else {
                if (selectedStoryIndex != -1) {
                    // Play stories starting from the selected story index
                    selectedStoryIndex
                } else {
                    // If no story is selected, start from the beginning
                    0
                }
            }

            // Set the duration based on the type of story (video or image)
            stories = resources[counter]
            if (stories.type == "video") {
                binding.stories.setStoryDuration(
                    ((stories.duration?.toFloat() ?: 0f) * STORY_DURATION).toLong()
                )
                startVideo(stories.video_url ?: "")
            } else {
                binding.stories.setStoryDuration(STORY_DURATION_FOR_IMAGE)
                showImage(stories.file_path ?: "")
            }

            // Start playing the stories
            binding.stories.startStories(counter)
            val loggedInUserId = loggedInUserCache.getLoggedInUserId()
            binding.replyview.isVisible = loggedInUserId != stories.user_id
            binding.storyViews.isVisible = loggedInUserId == stories.user_id
        }
    }

    private fun showImage(imageOrVideoUrl: String) {
        pauseStory()
        setInitialVisibility()
        setStoryDetails()

        setMusicInfo()
        loadImage(imageOrVideoUrl)
        handleWebLink()
    }

    private fun pauseStory() {
        Handler(Looper.myLooper()!!).post { binding.stories.pause() }
    }

    private fun setInitialVisibility() {
        binding.progressBar.visibility = View.VISIBLE
        binding.playerView.visibility = View.GONE
        binding.image.visibility = View.VISIBLE
    }

    private fun setStoryDetails() {
        binding.tvPostDateTime.text = resources[counter].created_at
        binding.tvUsername.text = storyListResponse?.userName
        binding.txtStoryViewers.text = requireContext().resources.getString(
            R.string.number_Viewers,
            resources[counter].no_of_views_count.toString()
        )

        binding.ivAccountVerified.visibility =
            if (resources[counter].user?.isVerified == 1) View.VISIBLE else View.GONE
    }

    private fun setMusicInfo() {
        if (!resources[counter].music_title.isNullOrEmpty()) {
            binding.llMusicInfo.isVisible = true
            Glide.with(requireActivity()).asGif().load(R.raw.music).into(binding.ivMusicLyricsWav)
            val musicName = resources[counter].music_title.plus(", ").plus(resources[counter].artists)
            binding.tvMusicName.text = musicName
        } else {
            binding.llMusicInfo.isVisible = false
        }
    }

    private fun loadImage(imageOrVideoUrl: String) {
        if (imageOrVideoUrl.isNotBlank()) {
            Glide.with(requireContext()).load(imageOrVideoUrl).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.stories.setStoryDuration(STORY_DURATION_FOR_IMAGE)
                    binding.progressBar.visibility = View.GONE
                    Handler(Looper.myLooper()!!).post { binding.stories.resume() }

                    adjustImageSize(resource)
                    return false
                }
            }).into(binding.image)
        }
    }

    private fun adjustImageSize(resource: Drawable?) {
        val width = resource?.intrinsicWidth
        val height = resource?.intrinsicHeight
        if (width != null && height != null) {
            binding.linkAttachmentContainer.layoutParams?.apply {
                this.height = height
                this.width = width
            }
        }
    }

    private fun handleWebLink() {
        if (!resources[counter].web_link.isNullOrEmpty()) {
            setupLinkView()
        } else {
            clearLinkAttachmentContainer()
        }
    }

    private fun setupLinkView() {
        val inputString = resources[counter].position
        binding.linkAttachmentContainer.visibility = View.VISIBLE
        val trimmedString = inputString?.trim('[', ']')
        val stringArray = trimmedString?.split(",")
        val doubleList = stringArray?.map { it.toDouble() }
        val lastX = doubleList?.get(0)?.toFloat() ?: 0f
        val lastY = doubleList?.get(1)?.toFloat() ?: 0f
        val buttonWidth = doubleList?.get(2)?.toInt() ?: 0
        val buttonHeight = doubleList?.get(THIRD)?.toInt() ?: 0
        val rotation = resources[counter].rotationAngle ?: 0.0f

        val view = LayoutInflater.from(context).inflate(
            R.layout.link_click_view,
            binding.linkAttachmentContainer,
            false
        )
        setupLinkViewPosition(view, lastX, lastY, buttonWidth, buttonHeight)
        view.rotation = rotation

        binding.linkAttachmentContainer.removeAllViews()
        binding.linkAttachmentContainer.addView(view)
        view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
            openUrlInBrowser(resources[counter].web_link!!)
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun setupLinkViewPosition(view: View, lastX: Float, lastY: Float, buttonWidth: Int, buttonHeight: Int) {
        if (lastX != 0f && lastY != 0f) {
            view.x = lastX
            view.y = lastY
            view.layoutParams = view.layoutParams.apply {
                width = buttonWidth
                height = buttonHeight
            }
        } else {
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                marginEnd = FIX_MARGIN
                marginStart = FIX_MARGIN
                width = buttonWidth
                height = buttonHeight
            }
            view.layoutParams = layoutParams
        }
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.android.chrome")
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // Chrome is not installed, open with any available browser
            intent.setPackage(null)
            startActivity(intent)
        }
    }

    private fun startVideo(url: String) {
        if (!isPlayerInitialized) {
            initPlayer()
            if (!isPlayerInitialized) return
        }

        // Ensure the URL is valid
        if (!URLUtil.isValidUrl(url)) {
            requireActivity().showToast("Invalid video URL.")
            return
        }
        binding.stories.pauseStory(counter)
        binding.progressBar.visibility = View.VISIBLE
        binding.playerView.visibility = View.VISIBLE
        binding.image.visibility = View.GONE
        binding.tvPostDateTime.text = resources[counter].created_at
        binding.tvUsername.text = storyListResponse?.userName
        binding.txtStoryViewers.text = requireContext().resources.getString(
            R.string.number_Viewers,
            resources[counter].no_of_views_count.toString()
        )

        if (resources[counter].user?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }
        if (!resources[counter].music_title.isNullOrEmpty()) {
            binding.llMusicInfo.isVisible = true
            Glide.with(requireActivity()).asGif().load(R.raw.music).into(binding.ivMusicLyricsWav)
            val musicName = resources[counter].music_title.plus(", ").plus(resources[counter].artists)
            binding.tvMusicName.text = musicName
        } else {
            binding.llMusicInfo.isVisible = false
        }
        buildMediaSource(("$url?clientBandwidthHint=2.5").toUri())
        videoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(
                videoSize: VideoSize
            ) {
                val videoHeight = videoSize.height
                binding.linkAttachmentContainer.layoutParams.apply {
                    this.height = videoHeight
                }
                if (!stories.web_link.isNullOrEmpty()) {
                    val inputString = resources[counter].position
                    binding.linkAttachmentContainer.visibility = View.VISIBLE
                    val trimmedString = inputString?.trim('[', ']')
                    val stringArray = trimmedString?.split(",")
                    val doubleList = stringArray?.map { it.toDouble() }
                    val lastX = doubleList?.get(0)?.toFloat() ?: 0f
                    val lastY = doubleList?.get(1)?.toFloat() ?: 0f
                    var buttonWidth = doubleList?.get(2)?.toInt() ?: 0
                    var buttonHeight = doubleList?.get(THIRD)?.toInt() ?: 0
                    val fullScreenHeight = requireActivity().resources.displayMetrics.heightPixels
                    val fullScreenWidth = requireActivity().resources.displayMetrics.widthPixels
                    val newHeight = binding.linkAttachmentContainer.layoutParams.height.toFloat()
                    val buttonX = (lastX * (fullScreenWidth)) / HUNDRED
                    val buttonY = (lastY * (newHeight)) / HUNDRED
                    buttonWidth = ((buttonWidth * fullScreenWidth) / HUNDRED)
                    buttonHeight = ((buttonHeight * fullScreenHeight) / HUNDRED)
                    val rotation = resources[counter].rotationAngle ?: 0.0f
                    val view = LayoutInflater.from(
                        context
                    ).inflate(R.layout.link_click_view, binding.linkAttachmentContainer, false)

                    if (lastX != 0f && lastY != 0f) {
                        view.x = buttonX
                        view.y = buttonY
                        // Set the dimensions
                        val layoutParams = view.layoutParams
                        layoutParams.width = buttonWidth
                        layoutParams.height = buttonHeight
                        view.layoutParams = layoutParams
                    } else {
                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
                            marginEnd = FIX_MARGIN
                            marginStart = FIX_MARGIN
                            width = buttonWidth.toInt()
                            height = buttonHeight.toInt()
                        }
                        view.layoutParams = layoutParams
                    }
                    view.rotation = rotation
                    binding.linkAttachmentContainer.removeAllViews()
                    binding.linkAttachmentContainer.addView(view)
                    view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
                        openUrlInBrowser(resources[counter].web_link!!)
                    }
                } else {
                    if (binding.linkAttachmentContainer.childCount > 0) {
                        binding.linkAttachmentContainer.removeAllViews()
                        binding.linkAttachmentContainer.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun buildMediaSource(mUri: Uri) {
        try {
            val dataSourceFactory: com.google.android.exoplayer2.upstream.DataSource.Factory =
                DefaultDataSourceFactory(requireContext(), getString(R.string.app_name))
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mUri))
            videoPlayer.setMediaSource(mediaSource)
            videoPlayer.prepare()
            videoPlayer.playWhenReady = true
            videoPlayer.addListener(object : Player.Listener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == Player.STATE_READY && playWhenReady && visible) {
                        binding.stories.resumeStory(counter)
                        binding.stories.setDurationStory(videoPlayer.duration, counter)
                        binding.progressBar.isVisible = false
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    videoPlayer.stop()
                }
            })
        } catch (_: java.lang.Exception) {
        }
    }

    override fun onNext() {
        binding.edittext.text?.clear()
        clearLinkAttachmentContainer()
        stopVideoPlayer()

        if (!this::resources.isInitialized) return

        counter++
        handleStoryView()

        if (counter < size) {
            handleStoryResources()
        }
    }

    private fun clearLinkAttachmentContainer() {
        if (binding.linkAttachmentContainer.childCount > 0) {
            binding.linkAttachmentContainer.removeAllViews()
            binding.linkAttachmentContainer.visibility = View.GONE
        }
    }

    private fun stopVideoPlayer() {
        if (this::videoPlayer.isInitialized) {
            videoPlayer.stop()
        }
    }

    private fun handleStoryView() {
        val loggedInUserId = loggedInUserCache.getLoggedInUserId()
        if (loggedInUserId != storyListResponse?.id) {
            mainHomeViewModel.viewStory(resources[counter].id.toString())
        }
    }

    private fun handleStoryResources() {
        stories = if (resources[counter].type == "video") {
            resources[counter].video_url?.let {
                startVideo(it)
            }
            resources[counter]
        } else {
            resources[counter].file_path?.let {
                binding.stories.setStoryDuration(STORY_DURATION_FOR_IMAGE)
                showImage(it)
            }
            resources[counter]
        }
    }

    override fun onPrev() {
        binding.edittext.text?.clear()
        clearLinkAttachmentContainer()
        stopVideoPlayer()

        if (!this::resources.isInitialized || counter <= 0) return

        counter--
        handlePreviousStory()
    }

    private fun handlePreviousStory() {
        if (counter < 0) return
        if (counter < size) {
            stories = if (resources[counter].type == "image") {
                resources[counter].file_path?.let { showImage(it) }
                resources[counter]
            } else {
                resources[counter].video_url?.let { startVideo(it) }
                resources[counter]
            }
        }
    }

    override fun onComplete() {
        onClick?.let { onClick -> onClick(position) }
    }

    override fun onDestroy() {
        binding.stories.destroy()
        videoPlayer.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        visible = isVisible
        if (isVisible && resources.isNotEmpty()) {
            startStories(resources as ArrayList<StoriesResponse>)
        }
    }

    override fun onPause() {
        super.onPause()
        visible = false
        binding.stories.pause()
        videoPlayer.pause()
        savedPosition = counter
    }

    override fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    override fun showLoading() {
        CallProgressWheel.showLoadingDialog(requireContext())
    }

    override fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }

    fun showToast(msg: CharSequence) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}

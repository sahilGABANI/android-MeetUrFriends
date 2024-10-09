package com.meetfriend.app.ui.challenge

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TimePicker
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.CountryModel
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.post.model.HashTagsResponse
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityCreateChallengeBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getSelectedHashTags
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.SnapkitActivity
import com.meetfriend.app.ui.challenge.bottomsheet.ChooseCityFragment
import com.meetfriend.app.ui.challenge.bottomsheet.ChooseCountryFragment
import com.meetfriend.app.ui.challenge.bottomsheet.ChooseStateFragment
import com.meetfriend.app.ui.challenge.viewmodel.AddNewChallengeViewState
import com.meetfriend.app.ui.challenge.viewmodel.CreateChallengeViewModel
import com.meetfriend.app.ui.chatRoom.roomview.view.MentionUserAdapter
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.WhoCanWatchBottomsheet
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.ShareHelper
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateChallengeActivity : BasicActivity() {

    companion object {
        const val REQUEST_FOR_CHOOSE_PHOTO = 18889
        const val HASH_TAG_NAME = "HASH_TAG_NAME"
        const val TIMEOUT = 400L
        const val SIX = 6
        const val TIME_US = 1000000L
        const val TWELVE = 12
        const val FIFTEEN = 15
        const val NINETY = 90
        const val TWENTY_SIX = 26

        private const val TAG = "CreateChallengeActivity"

        fun getIntent(context: Context, hashTagName: String? = null): Intent {
            val intent = Intent(context, CreateChallengeActivity::class.java)

            if (!hashTagName.isNullOrEmpty()) {
                intent.putExtra(HASH_TAG_NAME, hashTagName)
            }

            return intent
        }
    }

    private var thumbnailPath: String? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateChallengeViewModel>
    private lateinit var createChallengeViewModel: CreateChallengeViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var listData: java.util.HashMap<Int, Pair<String?, String?>>? = null
    private lateinit var binding: ActivityCreateChallengeBinding
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH)
    private val day = calendar.get(Calendar.DAY_OF_MONTH)
    private var selectedCountry: ArrayList<CountryModel> = arrayListOf()
    private var selectedStates: ArrayList<CountryModel> = arrayListOf()
    private var countryId: SpannableStringBuilder? = null
    private var statesId: SpannableStringBuilder? = null
    private var cityId: SpannableStringBuilder? = null
    private var seenId: Int = 1
    private var mediaPath: String? = null

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    private var scrollCount = 0
    private var selectedHashTagUserInfo: MutableList<HashTagsResponse> = mutableListOf()
    private lateinit var mentionUserAdapter: MentionUserAdapter
    private var optionIsSelect: String? = "Followers"
    private var linkAttachmentDetails: LinkAttachmentDetails? = null

    private var musicResponse: MusicInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)

        binding = ActivityCreateChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createChallengeViewModel = getViewModelFromFactory(viewModelFactory)
        initUI()
    }

    private fun initUI() {
        intent?.let {
            if (it.hasExtra(HASH_TAG_NAME)) {
                binding.edtDescription.setText(
                    "#${
                        it.getStringExtra(HASH_TAG_NAME)?.replace("#", "")
                    } "
                )
            }
        }
        listenToViewModel()
        setmentionUserAdapter()
        setupWhoCanWatchListener()

        binding.rvHashtagList.apply {
            layoutManager = LinearLayoutManager(
                this@CreateChallengeActivity,
                LinearLayoutManager.VERTICAL, false
            )
            adapter = mentionUserAdapter
        }

        binding.edtDescription.textChanges()
            .debounce(TIMEOUT, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.llHashTagListContainer.visibility = View.GONE
                    scrollCount = 0
                } else {
                    val wordList = it.split(" ")
                    val lastWord = wordList.last()
                    val searchHash: String = lastWord.substringAfterLast("#")

                    if (lastWord.contains("#")) {
                        createChallengeViewModel.getHashTagList(searchHash)
                    } else {
                        binding.llHashTagListContainer.visibility = View.GONE
                    }
                }
            }.autoDispose()
        setupSaveButton()
        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
        setDateTimeButton()
        setupMediaUpload()
        setupAutoCompleteStatus()
        setEditCountry()
        setEditCity()
        setEditState()
    }

    private fun setEditState() {
        binding.edtState.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            if (selectedCountry.size > 0) {
                countryId = SpannableStringBuilder("")
                selectedCountry.forEach {
                    if (it == selectedCountry.lastOrNull()) {
                        countryId?.append(it.id.toString())
                    } else {
                        countryId?.append("${it.id}, ")
                    }
                }

                if (selectedCountry.find { it.sortName == "All" } == null) {
                    val chooseStateFragment =
                        ChooseStateFragment.newInstance(countryId.toString()).apply {
                            countryItemClick.subscribeAndObserveOnMainThread { item ->
                                selectedStates = item as ArrayList<CountryModel>
                                val countryName = SpannableStringBuilder("")

                                var listState =
                                    if (item.find { it.sortName == "All" } != null) {
                                        arrayListOf<CountryModel>(
                                            item.get(0)
                                        )
                                    } else {
                                        item
                                    }

                                listState.forEach {
                                    if (it == listState.lastOrNull()) {
                                        countryName.append(it.name)
                                    } else {
                                        countryName.append("${it.name}, ")
                                    }
                                }
                                binding.edtState.text = countryName

                                if (item.find { it.sortName == "All" } != null) {
                                    statesId = SpannableStringBuilder("1")
                                    cityId = SpannableStringBuilder("1")
                                    binding.edtCity.text = SpannableStringBuilder("All")
                                }
                                dismiss()
                            }.autoDispose()
                        }
                    chooseStateFragment.show(
                        supportFragmentManager,
                        ChooseStateFragment::class.java.name
                    )
                }
            } else {
                if (selectedStates.find { it.sortName == "All" } == null) {
                    showToast(
                        resources.getString(
                            R.string.label_please_select_country
                        )
                    )
                }
            }
        }.autoDispose()
    }

    private fun setEditCity() {
        binding.edtCity.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            if (selectedStates.size > 0) {
                statesId = SpannableStringBuilder("")
                selectedStates.forEach {
                    if (it == selectedStates.lastOrNull()) {
                        statesId?.append(it.id.toString())
                    } else {
                        statesId?.append("${it.id}, ")
                    }
                }

                if (selectedStates.find { it.sortName == "All" } == null) {
                    val chooseCityFragment =
                        ChooseCityFragment.newInstance(statesId.toString()).apply {
                            countryItemClick.subscribeAndObserveOnMainThread { item ->
                                val countryName = SpannableStringBuilder("")
                                cityId = SpannableStringBuilder("")

                                var listCity =
                                    if (item.find { it.sortName == "All" } != null) {
                                        arrayListOf<CountryModel>(
                                            item.get(0)
                                        )
                                    } else {
                                        item
                                    }
                                listCity.forEach {
                                    if (it == listCity.lastOrNull()) {
                                        countryName.append(it.name)
                                        cityId?.append(it.id.toString())
                                    } else {
                                        countryName.append("${it.name}, ")
                                        cityId?.append("${it.id}, ")
                                    }
                                }

                                if (item.find { it.sortName == "All" } != null) {
                                    cityId = SpannableStringBuilder("1")
                                }
                                binding.edtCity.text = countryName

                                dismiss()
                            }.autoDispose()
                        }
                    chooseCityFragment.show(
                        supportFragmentManager,
                        ChooseCityFragment::class.java.name
                    )
                }
            } else {
                if (selectedCountry.find { it.sortName == "All" } == null) {
                    showToast(resources.getString(R.string.label_please_select_state))
                }
            }
        }.autoDispose()
    }

    private fun setupAutoCompleteStatus() {
        val list = listOf("Everyone", "Following", "Followers")
        val arrayAdapter = ArrayAdapter(
            this@CreateChallengeActivity,
            android.R.layout.simple_spinner_dropdown_item,
            list
        )
        binding.autoCompleteStatus.setAdapter(arrayAdapter)
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            binding.autoCompleteStatus.isSelected = true
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                binding.autoCompleteStatus.isSelected = false
                seenId = position + 1
            }
    }

    private fun setDateTimeButton() {
        binding.edtDate.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            openDatePickerDialog(true)
        }.autoDispose()
        binding.edtEndDate.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            openDatePickerDialog(false)
        }.autoDispose()
        binding.edtTime.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            openTimePickerDialog(true)
        }.autoDispose()
        binding.edtEndTime.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            openTimePickerDialog(false)
        }.autoDispose()
    }

    private fun setEditCountry() {
        binding.edtCountry.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            val chooseCountryFragment = ChooseCountryFragment.newInstance().apply {
                countryItemClick.subscribeAndObserveOnMainThread { item ->
                    if (item.isNotEmpty()) {
                        selectedCountry = item as ArrayList<CountryModel>
                        val countryName = SpannableStringBuilder("")

                        var listCountry =
                            if (item.find { it.sortName == "All" } != null) {
                                arrayListOf<CountryModel>(
                                    item.get(0)
                                )
                            } else {
                                item
                            }
                        listCountry.forEach {
                            if (it == listCountry.lastOrNull()) {
                                countryName.append(it.name)
                            } else {
                                countryName.append("${it.name}, ")
                            }
                        }
                        binding.edtCountry.text = countryName

                        if (item.find { it.sortName == "All" } != null) {
                            countryId = SpannableStringBuilder("1")
                            statesId = SpannableStringBuilder("1")
                            cityId = SpannableStringBuilder("1")
                            binding.edtState.text = SpannableStringBuilder("All")
                            binding.edtCity.text = SpannableStringBuilder("All")
                        }
                        dismiss()
                    }
                }.autoDispose()
            }
            chooseCountryFragment.show(
                supportFragmentManager,
                ChooseCountryFragment::class.java.name
            )
        }.autoDispose()
    }

    private fun setupMediaUpload() {
        binding.llUpload.throttleClicks().subscribeAndObserveOnMainThread {
            binding.edtChallengeTitle.clearFocus()
            binding.edtDescription.clearFocus()
            XXPermissions.with(this@CreateChallengeActivity)
                .permission(Permission.CAMERA)
                .permission(Permission.RECORD_AUDIO)
                .permission(Permission.READ_MEDIA_IMAGES)
                .permission(Permission.READ_MEDIA_VIDEO)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) {
                            startActivityForResult(
                                SnapkitActivity.getIntent(
                                    this@CreateChallengeActivity,
                                    false,
                                    isChallenge = true
                                ),
                                REQUEST_FOR_CHOOSE_PHOTO
                            )
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        super.onDenied(permissions, never)
                        if (never) {
                            showToast("Authorization is permanently denied, please manually grant permissions")
                            XXPermissions.startPermissionActivity(
                                this@CreateChallengeActivity,
                                permissions
                            )
                        } else {
                            showToast(getString(R.string.msg_permission_denied))
                        }
                    }
                })
        }
    }

    private fun setupSaveButton() {
        binding.saveAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val allowUsers = listData?.keys
            if (isValidate()) {
                val timeZone = if (Build.VERSION.SDK_INT >= TWENTY_SIX) {
                    ZonedDateTime.now().zone.toString()
                } else {
                    TimeZone.getDefault().id
                }
                setMultipartBody(allowUsers, timeZone)
            }

            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()
    }
    private fun addBasicFormData(builder: MultipartBody.Builder, timeZone: String) {
        builder.addFormDataPart("privacy", seenId.toString())
        builder.addFormDataPart("country", (countryId ?: "").toString())
        builder.addFormDataPart("state", (statesId ?: "").toString())
        builder.addFormDataPart("city", (cityId ?: "").toString())
        builder.addFormDataPart("time_from", binding.edtTime.text.toString())
        builder.addFormDataPart("date_from", binding.edtDate.text.toString())
        builder.addFormDataPart("time_to", binding.edtEndTime.text.toString())
        builder.addFormDataPart("date_to", binding.edtEndDate.text.toString())
        builder.addFormDataPart("width", contentWidth.toString())
        builder.addFormDataPart("height", contentHeight.toString())
        builder.addFormDataPart("timezone", timeZone)
        builder.addFormDataPart("platform", "Android")
    }
    private fun addLinkAttachmentDetails(builder: MultipartBody.Builder) {
        linkAttachmentDetails?.let {
            val listOfPositionArray = arrayListOf<Float>().apply {
                add(it.finalX ?: 0.0F)
                add(it.finalY ?: 0.0F)
                add(it.lastWidth ?: 0.0F)
                add(it.lastHeight ?: 0.0F)
            }
            builder.addFormDataPart("position", listOfPositionArray.toString())
            builder.addFormDataPart("rotation_angle", it.lastRotation.toString())
            builder.addFormDataPart("web_link", it.attachUrl.toString())
        }
    }
    private fun setMultipartBody(allowUsers: MutableSet<Int>?, timeZone: String) {
        val builder = MultipartBody.Builder()
        // Add fixed form data
        addBasicFormData(builder, timeZone)
        builder.setType(MultipartBody.FORM)
        if (!binding.edtChallengeTitle.text.toString().isNullOrEmpty()) {
            builder.addFormDataPart("title", binding.edtChallengeTitle.text.toString())
        }
        if (!binding.edtDescription.text.toString().isNullOrEmpty()) {
            builder.addFormDataPart("description", binding.edtDescription.text.toString())
        }
        // Add link attachment details if available
        addLinkAttachmentDetails(builder)
        if (musicResponse != null) {
            musicResponse?.name?.let { it1 ->
                builder.addFormDataPart("music_title", it1)
            }
            builder.addFormDataPart("artists", getArtistsNames() ?: "")
        }
        builder.addFormDataPart("country_code", resources.configuration.locales.get(0).country)
        builder.addFormDataPart(
            "tag_names",
            getSelectedHashTags(binding.edtDescription.text.toString()) ?: ""
        )
        builder.addFormDataPart(
            "allowed_users",
            if (allowUsers?.isNotEmpty() == true) {
                TextUtils.join(",", allowUsers)
            } else {
                ""
            }
        )
        // Map is used to multipart the file using okhttp3.RequestBody
        // Multiple Images

        mediaPath?.let {
            val file = File(it)
            builder.addFormDataPart(
                "media",
                file.name,
                file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            )
        }

        if (!thumbnailPath.isNullOrEmpty()) {
            val thumbnailFile = File(thumbnailPath.toString())
            builder.addFormDataPart(
                "thumbnail",
                thumbnailFile.name,
                thumbnailFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            )
        }

        val requestBody = builder.build()

        createChallengeViewModel.createPost(requestBody)
    }

    private fun setupWhoCanWatchListener() {
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
                        binding.tvWhoCanWatch.text =
                            it.list?.values?.map { it.first }?.joinToString(separator = ", ")
                    }
                }
            }.autoDispose()
            bottomsheet.show(this.supportFragmentManager, AddNewPostInfoActivity::class.java.name)
        }
    }

    private fun setmentionUserAdapter() {
        mentionUserAdapter = MentionUserAdapter(this).apply {
            hashTagClicks.subscribeAndObserveOnMainThread { mentionUser ->
                val cursorPosition: Int = binding.edtDescription.selectionStart
                val descriptionString = binding.edtDescription.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                createChallengeViewModel.searchHashTagClicked(
                    binding.edtDescription.text.toString(),
                    subString,
                    mentionUser
                )
                if (mentionUser !in selectedHashTagUserInfo) {
                    selectedHashTagUserInfo.add(mentionUser)
                }
            }.autoDispose()
        }
    }

    private fun isValidate(): Boolean {
        val errorMessage = when {
            mediaPath == null -> "Please select photo/video before create challenge"
            binding.edtDate.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_start_date)

            binding.edtTime.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_start_time)

            binding.edtEndDate.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_end_date)

            binding.edtEndTime.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_end_time)

            binding.edtCountry.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_country)

            binding.edtState.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_state)

            binding.edtCity.text.toString().isNullOrEmpty() ->
                resources.getString(R.string.label_select_challenge_city)

            else -> null
        }

        return if (errorMessage != null) {
            showToast(errorMessage)
            false
        } else {
            true
        }
    }

    fun getArtistsNames(): String? {
        return musicResponse?.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
    }

    private fun openDatePickerDialog(isStartDate: Boolean) {
        val datePickerDialog =
            DatePickerDialog(this@CreateChallengeActivity, { _, year, monthOfYear, dayOfMonth ->
                if (isStartDate) {
                    val paddedMonth = String.format(Locale.getDefault(), "%02d", monthOfYear + 1)
                    val dayMonth = String.format(Locale.getDefault(), "%02d", dayOfMonth)

                    binding.edtDate.setText("$year-$paddedMonth-$dayMonth")
                } else {
                    val paddedMonth = String.format(Locale.getDefault(), "%02d", monthOfYear + 1)
                    val dayMonth = String.format(Locale.getDefault(), "%02d", dayOfMonth)
                    binding.edtEndDate.setText("$year-$paddedMonth-$dayMonth")
                }
            }, year, month, day)
        val maximumDate = Calendar.getInstance()
        maximumDate.set(Calendar.DAY_OF_MONTH, day)
        maximumDate.set(Calendar.MONTH, month)
        maximumDate.set(Calendar.YEAR, year)

        val minimumDate = Calendar.getInstance()
        minimumDate.set(Calendar.DAY_OF_MONTH, day + FIFTEEN)
        minimumDate.set(Calendar.MONTH, month)
        minimumDate.set(Calendar.YEAR, year)

        datePickerDialog.datePicker.minDate = maximumDate.timeInMillis
        datePickerDialog.datePicker.maxDate = minimumDate.timeInMillis
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
        datePickerDialog.show()
    }

    private fun mentionUserViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.llHashTagListContainer.visibility == View.GONE) {
            binding.llHashTagListContainer.visibility = View.VISIBLE
            if (scrollCount == 0) {
                binding.nestedScrollView.smoothScrollTo(
                    0,
                    binding.descriptionTextInputLayout.bottom
                )
                scrollCount++
            }
        } else if (!isVisibility && binding.llHashTagListContainer.visibility == View.VISIBLE) {
            binding.llHashTagListContainer.visibility = View.GONE
            scrollCount = 0
        }
    }

    private fun listenToViewModel() {
        createChallengeViewModel.addNewChallengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewChallengeViewState.HashTagListForMention -> {
                    mentionUserViewVisibility(!it.listOfUserForMention.isNullOrEmpty())
                    mentionUserAdapter.listOfHashTags = it.listOfUserForMention
                    mentionUserAdapter.listOfDataItems = null
                }

                is AddNewChallengeViewState.UpdateDescriptionText -> {
                    mentionUserViewVisibility(false)
                    binding.edtDescription.setText(it.descriptionString)
                    binding.edtDescription.setSelection(binding.edtDescription.text.toString().length)
                }

                is AddNewChallengeViewState.LoadingState -> {
                    if (it.isLoading) {
                        showLoading()
                    } else {
                        hideLoading()
                    }
                }

                is AddNewChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is AddNewChallengeViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }

                is AddNewChallengeViewState.CreatePostSuccessMessage -> {
                    val type = "Challenge count"
                    mp?.people?.increment(type, 1.0)

                    val props = JSONObject()
                    props.put("content_type", "challenge")
                    it.challengeId?.let { postId ->
                        ShareHelper.shareDeepLink(
                            this,
                            SIX,
                            postId,
                            "",
                            false
                        ) { link ->
                            props.put("link", link)
                            mp?.track("Create Content", props)
                        }
                    }
                    finish()
                }

                else -> {}
            }
        }
    }

    private fun openTimePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(
            this,
            { _: TimePicker?, hourOfDay: Int, minutes: Int ->
                if (isStartDate) {
                    binding.edtTime.setText(formatTime(hourOfDay, minutes))
                } else {
                    binding.edtEndTime.setText(formatTime(hourOfDay, minutes))
                }
            },
            hour,
            minute,
            false // set to true if you want to use 24-hour format
        )
        timePickerDialog.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < TWELVE) "AM" else "PM"
        val formattedHour = if (hour % TWELVE == 0) TWELVE else hour % TWELVE
        return String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, minute, amPm)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_FOR_CHOOSE_PHOTO) {
            val filePath = data?.getStringExtra("FILE_PATH")
            contentHeight = data?.getIntExtra("HEIGHT", 0) ?: 0
            contentWidth = data?.getIntExtra("WIDTH", 0) ?: 0
            musicResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("MUSIC_INFO", MusicInfo::class.java)
            } else {
                data?.getParcelableExtra("MUSIC_INFO")
            }
            mediaPath = filePath
            val linkAttachmentDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(
                    "LINK_ATTACHMENT_DETAILS",
                    LinkAttachmentDetails::class.java
                )
            } else {
                data?.getParcelableExtra("LINK_ATTACHMENT_DETAILS")
            }
            this.linkAttachmentDetails = linkAttachmentDetails
            binding.photoAppCompatImageView.visibility = View.VISIBLE
            binding.uploadLinearLayout.visibility = View.GONE
            Glide.with(this@CreateChallengeActivity)
                .load(mediaPath)
                .centerCrop()
                .into(binding.photoAppCompatImageView)

            if (isVideoFile(mediaPath)) {
                mediaPath?.let { it1 ->
                    thumbnailPath = saveVideoThumbnail(this, it1, "video_thumbnail")
                    Timber.tag(TAG).i("thumbnailPath :$thumbnailPath")
                }
            }
        }
    }

    fun isVideoFile(filePath: String?): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            retriever.release()
            hasVideo != null && hasVideo == "yes"
        } catch (e: IOException) {
            Timber.tag(TAG).i("Error :$e")
            retriever.release()
            false
        }
    }

    fun saveVideoThumbnail(context: Context, videoPath: String, outputFileName: String): String? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            // Extract a frame at the first second (1000000 microseconds)
            val bitmap =
                retriever.getFrameAtTime(TIME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            // Define the path to save the thumbnail
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, "$outputFileName.jpg")

            // Save the bitmap to a file
            FileOutputStream(file).use { fos ->
                bitmap?.compress(Bitmap.CompressFormat.JPEG, NINETY, fos)
            }

            Timber.tag(TAG).i("thumbnailPath :${file.absolutePath}")
            return file.absolutePath
        } catch (e: IOException) {
            Timber.tag(TAG).i("Error :$e")
            return null
        } finally {
            retriever.release()
        }
    }
}

package com.meetfriend.app.ui.myprofile

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RadioButton
import androidx.core.view.isVisible
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityEditProfileBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showKeyboard
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.activities.FullScreenActivity
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.ui.home.suggestion.UserSuggestionActivity
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewModel
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewState
import com.meetfriend.app.utilclasses.AppConstants
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.CAMERA
import com.meetfriend.app.utils.Constant.COVER
import com.meetfriend.app.utils.Constant.FOLDER
import com.meetfriend.app.utils.Constant.FiXED_4_INT
import com.meetfriend.app.utils.Constant.FiXED_MINUS_13_INT
import com.meetfriend.app.utils.Constant.MAXIMUM_4_LINES_ARE_ALLOWED
import com.meetfriend.app.utils.Constant.STRING_NULL
import com.meetfriend.app.utils.Constant.TAP_TO_SELECT
import com.meetfriend.app.utils.FileUtils
import contractorssmart.app.utilsclasses.CommonMethods
import contractorssmart.app.utilsclasses.PreferenceHandler
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

class EditProfileActivity : BasicActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    val myCalendar: Calendar = Calendar.getInstance()
    private var genderSelectedValue = ""
    private var relationShipValue = ""
    private var imageType = ""
    private var profilePicture = ""
    private var isFromSplash = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserProfileViewModel>
    private lateinit var userProfileViewModel: UserProfileViewModel
    private lateinit var handlePathOz: HandlePathOz

    companion object {
        const val INTENT_FROM_SPLASH = "INTENT_FROM_SPLASH"
        const val USER_DATA = "USER_DATA"
        const val PROFILE_PICTURE = "profilePicture"
        const val NAME = "name"
        const val URL = "url"
        fun getIntent(
            context: Context,
            isFromSplash: Boolean,
            meetFriendUser: MeetFriendUser? = null
        ): Intent {
            val intent = Intent(context, EditProfileActivity::class.java)
            intent.putExtra(INTENT_FROM_SPLASH, isFromSplash)
            val gson = Gson()
            val json: String = gson.toJson(meetFriendUser)
            intent.putExtra(USER_DATA, json)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userProfileViewModel = getViewModelFromFactory(viewModelFactory)

        intent?.let {
            isFromSplash = it.getBooleanExtra(INTENT_FROM_SPLASH, false)
            val userProfile = it.getStringExtra(USER_DATA)
            val gson = Gson()
            val userData = gson.fromJson(userProfile, MeetFriendUser::class.java)
            setData(userData)
            if (isFromSplash) {
                manageEdit()
            }
        }

        initUI()
        listenToViewModel()
    }

    private fun manageEdit() {
        editDetailsEnable(true, boolean1 = true)
        binding.tvEditBio.isVisible = false
        binding.tvEditOtherDetails.isVisible = false
        binding.etBio.requestFocus(binding.etBio.layoutDirection)
        binding.tvBio.isVisible = false
    }

    private fun listenToViewModel() {
        userProfileViewModel.userProfileState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserProfileViewState.EditUserProfile -> {
                    CommonMethods.showToastMessageAtTop(this@EditProfileActivity, it.message)
                    if (isFromSplash) {
                        val userprofile = loggedInUserCache.getLoggedInUser()?.loggedInUser
                        userprofile?.apply {
                            city = binding.etCity.text.toString()
                            userName = binding.etUserName.text.toString()
                            isPrivate = if (binding.switchMakePrivate.isChecked) 1 else 0
                        }
                        loggedInUserCache.setLoggedInUser(userprofile)
                        startActivity(UserSuggestionActivity.getIntent(this))
                    } else {
                        val intent = Intent()
                        intent.putExtra(PROFILE_PICTURE, profilePicture)
                        intent.putExtra(
                            NAME,
                            "${binding.etFirstName.text.toString().trim()} ${
                                binding.etLastName.text.toString().trim()
                            }"
                        )
                        setResult(RESULT_OK, intent)
                    }
                    finish()
                }
                is UserProfileViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserProfileViewState.LoadingState -> {
                    showLoading(it.isLoading)
                }
                is UserProfileViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserProfileViewState.UploadUserProfileResponse -> {
                    showLoading(false)
                    CommonMethods.setImage(
                        binding.ivProfilePic,
                        it.updatePhotoResponse.profile_photo
                    )
                    PreferenceHandler.writeString(
                        this@EditProfileActivity,
                        PreferenceHandler.PROFILE_PHOTO,
                        it.updatePhotoResponse.media_url + it.updatePhotoResponse.profile_photo
                    )
                    val userprofile = loggedInUserCache.getLoggedInUser()?.loggedInUser
                    userprofile?.profilePhoto = it.updatePhotoResponse.profile_photo
                    loggedInUserCache.setLoggedInUser(userprofile)
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    private fun initUI() {
        handlePathOz = HandlePathOz(this@EditProfileActivity, listener)

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.etBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                return
            }

            override fun afterTextChanged(s: Editable?) {
                val lines = binding.etBio.lineCount
                if (lines > FiXED_4_INT) {
                    binding.etBio.text?.delete(
                        binding.etBio.layout.getLineStart(FiXED_4_INT),
                        binding.etBio.layout.getLineEnd(FiXED_4_INT)
                    )
                    binding.etBio.error = MAXIMUM_4_LINES_ARE_ALLOWED
                } else {
                    binding.etBio.error = null
                }
            }
        })
        if (!isFromSplash) {
            binding.etBio.isVisible = false
            binding.tvBio.isVisible = true
        }

        binding.ivEditProfilePic.setOnClickListener {
            openSelectionBottomSheet()
        }
        if (!isFromSplash) {
            editDetailsEnable(false, boolean1 = false)
        }
        binding.tvEditBio.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isFromSplash) {
                editDetailsEnable(true, boolean1 = false)
            }
            binding.etBio.isVisible = true
            binding.tvBio.isVisible = false
            binding.etBio.requestFocus(binding.etBio.layoutDirection)
            showKeyboard()
        }.autoDispose()
        binding.tvEditOtherDetails.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isFromSplash) {
                editDetailsEnable(false, boolean1 = true)
            }
            binding.etFirstName.requestFocus(binding.etFirstName.layoutDirection)
            showKeyboard()
        }.autoDispose()

        binding.tvUpdateInfo.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard()
            validations()
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()
    }

    private fun openSelectionBottomSheet() {
        val imageSelectionBottomSheet = ImagePickerOptionBottomSheet()
        imageSelectionBottomSheet.show(
            supportFragmentManager,
            ImagePickerOptionBottomSheet::class.java.name
        )

        imageSelectionBottomSheet.imageSelectionOptionClicks.subscribeAndObserveOnMainThread {
            when (it) {
                SelectedOption.Gallery.name -> {
                    checkPermissionGranted(this@EditProfileActivity)
                }

                SelectedOption.Camera.name -> {
                    checkPermissionGrantedForCamera(this)
                }
            }
        }.autoDispose()
    }

    private fun checkPermissionGrantedForCamera(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startActivityForResultWithDefaultAnimation(
                            PostCameraActivity.launchActivity(
                                this@EditProfileActivity,
                                true
                            ),
                            PostCameraActivity.RC_CAPTURE_PICTURE
                        )
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast(resources.getString(R.string.label_authorization_is_permanently_denied_please_manually_grant_permissions))
                        XXPermissions.startPermissionActivity(this@EditProfileActivity, permissions)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(this@EditProfileActivity)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    private fun editDetailsEnable(boolean: Boolean, boolean1: Boolean) {
        binding.etBio.isEnabled = boolean
        binding.etFirstName.isEnabled = boolean1
        binding.etLastName.isEnabled = boolean1
        binding.etCity.isEnabled = boolean1
        binding.etEducation.isEnabled = boolean1
        binding.etWork.isEnabled = boolean1
        binding.etDOB.isEnabled = boolean1
        binding.etHobbies.isEnabled = boolean1
        binding.radioMale.isClickable = boolean1
        binding.radioFemale.isClickable = boolean1
        binding.radioOther.isClickable = boolean1
        binding.radioSingle.isClickable = boolean1
        binding.radioMarried.isClickable = boolean1
        binding.radioUnmarried.isClickable = boolean1
        binding.etUserName.isEnabled = boolean1
        binding.switchMakePrivate.isClickable = boolean1
    }

    private fun setData(userData: MeetFriendUser? = null) {
        if (userData == null) {
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName != STRING_NULL) {
                binding.etFirstName.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName != STRING_NULL) {
                binding.etLastName.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName != STRING_NULL) {
                binding.etUserName.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.city != STRING_NULL) {
                binding.etCity.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.city ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.education != STRING_NULL) {
                binding.etEducation.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.education ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.work != STRING_NULL) {
                binding.etWork.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.work ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.dob != STRING_NULL) {
                binding.etDOB.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.dob ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.hobbies != STRING_NULL) {
                binding.etHobbies.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.hobbies ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.bio != STRING_NULL) {
                binding.etBio.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.bio ?: ""
                )
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.bio != STRING_NULL) {
                binding.tvBio.text =
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.bio ?: ""
            }
            genderSelectedValue = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender ?: ""
            relationShipValue =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.relationship ?: ""
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto != null) {
                Glide.with(this)
                    .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivProfilePic)
            }

            binding.switchMakePrivate.isChecked =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.isPrivate != 0

            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.emailOrPhone != STRING_NULL) {
                binding.etEmail.setText(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.emailOrPhone ?: ""
                )
            }
        } else {
            if (userData.firstName != STRING_NULL) binding.etFirstName.setText(userData.firstName ?: "")
            if (userData.lastName != STRING_NULL) binding.etLastName.setText(userData.lastName ?: "")
            if (userData.userName != STRING_NULL) binding.etUserName.setText(userData.userName ?: "")
            if (userData.city != STRING_NULL) binding.etCity.setText(userData.city ?: "")
            if (userData.education != STRING_NULL) binding.etEducation.setText(userData.education ?: "")
            if (userData.work != STRING_NULL) binding.etWork.setText(userData.work ?: "")
            if (userData.dob != STRING_NULL) binding.etDOB.setText(userData.dob ?: "")
            if (userData.hobbies != STRING_NULL) binding.etHobbies.setText(userData.hobbies ?: "")
            if (userData.bio != STRING_NULL) binding.etBio.setText(userData.bio ?: "")
            if (userData.emailOrPhone != STRING_NULL) binding.etEmail.setText(userData.emailOrPhone ?: "")
            genderSelectedValue = userData.gender ?: ""
            relationShipValue = userData.relationship ?: ""
            Glide.with(this).load(userData.profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivProfilePic)
        }
        when (genderSelectedValue) {
            resources.getString(R.string.label_male) -> {
                binding.radioMale.isChecked = true
            }
            resources.getString(R.string.label_female) -> {
                binding.radioFemale.isChecked = true
            }
            else -> {
                binding.radioOther.isChecked = true
            }
        }
        when (relationShipValue) {
            resources.getString(R.string.label_single) -> {
                binding.radioSingle.isChecked = true
            }
            resources.getString(R.string.label_married) -> {
                binding.radioMarried.isChecked = true
            }
            else -> {
                binding.radioUnmarried.isChecked = true
            }
        }
        binding.radioGroupGender.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<View>(checkedId) as RadioButton
            genderSelectedValue = rb.text.toString()
        }

        binding.radioRelationShip.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<View>(checkedId) as RadioButton
            relationShipValue = rb.text.toString()
        }

        binding.ivProfilePic.setOnClickListener {
            val intent = Intent(this@EditProfileActivity, FullScreenActivity::class.java)
            intent.putExtra(
                URL,
                getProfilePic()
            )
            startActivity(intent)
        }
        binding.ivEditProfilePic.setOnClickListener {
            imageType = resources.getString(R.string.label_profile)
            ImagePicker.create(this)
                .returnMode(
                    ReturnMode.ALL
                ) // set whether pick and / or camera action should return immediate result or not.
                .folderMode(true) // folder mode (false by default)
                .toolbarFolderTitle(FOLDER) // folder selection title
                .toolbarImageTitle(TAP_TO_SELECT) // image selection title
                .single() // single mode
                .showCamera(true) // show camera or not (true by default)
                .imageDirectory(CAMERA) // directory name for captured image  ("Camera" folder by default)
                .start()
        }
        binding.etDOB.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, FiXED_MINUS_13_INT)
            calendar.add(Calendar.MONTH, 0)
            calendar.add(Calendar.DAY_OF_MONTH, 0)
            SingleDateAndTimePickerDialog.Builder(this)
                .bottomSheet()
                .curved()
                .displayMinutes(false)
                .displayHours(false)
                .displayDays(false)
                .displayMonth(true)
                .displayYears(true)
                .displayDaysOfMonth(true)
                .maxDateRange(calendar.time)
                .listener(
                    SingleDateAndTimePickerDialog.Listener { date: Date? ->
                        val myCalendar = Calendar.getInstance()
                        myCalendar.time = date
                        if (calendar.time.before(myCalendar.time)) {
                            try {
                                CommonMethods.showToastMessageAtTop(
                                    this,
                                    resources.getString(R.string.label_age_can_not_be_less_then_13_year)
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            // dob_string=myCalendar.timeInMillis.toString()
                            // val myFormat = "dd-MM-yyyy" //In which you need put here
                            try {
                                val sdf = SimpleDateFormat(AppConstants.DOB_FORMAT, Locale.US)
                                binding.etDOB.setText(sdf.format(myCalendar.time))
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
                .display()
        }

        binding.ivEditCoverPic.setOnClickListener {
            imageType = COVER
            ImagePicker.create(this)
                .returnMode(
                    ReturnMode.ALL
                ) // set whether pick and / or camera action should return immediate result or not.
                .folderMode(true) // folder mode (false by default)
                .toolbarFolderTitle(FOLDER) // folder selection title
                .toolbarImageTitle(TAP_TO_SELECT) // image selection title
                .single() // single mode
                .showCamera(true) // show camera or not (true by default)
                .imageDirectory(CAMERA) // directory name for captured image  ("Camera" folder by default)
                .start()
        }
    }

    fun milliseconds(date: String?): Long {
        val sdf = SimpleDateFormat(AppConstants.DOB_FORMAT)
        try {
            val mDate = sdf.parse(date)
            val timeInMilliseconds = mDate.time
            return timeInMilliseconds
        } catch (e: ParseException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return 0
    }

    private fun validations() {
        val expression = "[0-9a-zA-Z._]*"
        val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)

        if (binding.etUserName.text.toString().trim().equals("")) {
            binding.etUserName.error = resources.getString(R.string.label_user_name_is_empty)
            showToast(resources.getString(R.string.label_user_name_is_empty))
            return
        } else if (!pattern.matcher(binding.etUserName.text.toString()).matches()) {
            binding.etUserName.error = resources.getString(R.string.label_special_character_or_space_is_not_allowed)
            showToast(resources.getString(R.string.label_special_character_or_space_is_not_allowed_in_username))
            return
        }
        binding.etUserName.error = null

        updateAPI()
    }

    var date = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
        myCalendar.set(Calendar.YEAR, year)
        myCalendar.set(Calendar.MONTH, monthOfYear)
        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        val myFormat = AppConstants.DOB_FORMAT // In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.etDOB.setText(sdf.format(myCalendar.time))
    }

    private fun updateAPI() {
        val userName = binding.etUserName.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val isPrivate = if (binding.switchMakePrivate.isChecked) 1 else 0
        userProfileViewModel.editUserProfile(
            userName = userName,
            bio = bio,
            isPrivate = isPrivate
        )
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                updateProfilePic(File(filePath))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        } else if ((requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) and (resultCode == RESULT_OK)) {
            val filePath = data?.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
            filePath?.let { updateProfilePic(File(it)) }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateProfilePic(file: File) {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        builder.addFormDataPart(
            "image",
            file.name,
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        )
        val requestBody = builder.build()
        userProfileViewModel.uploadProfileImage(requestBody)
    }
}

package com.meetfriend.app.ui.challenge

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChallengeCountRequest
import com.meetfriend.app.api.challenge.model.ChallengeItem
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityChallengeDetailsBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.challenge.bottomsheet.ChallengeCommentBottomSheetFragment
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utilclasses.UtilsClass
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ChallengeDetailsActivity : BasicActivity() {
    private lateinit var binding: ActivityChallengeDetailsBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var challengeId: Int = 0
    private lateinit var challengeItem: ChallengeItem

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeViewModel>
    private lateinit var challengeViewModel: ChallengeViewModel

    var reportDialog: Dialog? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted: Boolean = true
    private var currentLocation: String? = null
    private var country: String? = null
    private var city: String? = null
    private var state: String? = null

    companion object {
         const val INTENT_CHALLENGE_ID = "challengeId"
        fun getIntent(context: Context, challengeId: Int): Intent {
            val intent = Intent(context, ChallengeDetailsActivity::class.java)
            intent.putExtra(INTENT_CHALLENGE_ID, challengeId)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        binding = ActivityChallengeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        challengeViewModel = getViewModelFromFactory(viewModelFactory)
        intent?.let {
            challengeId = it.getIntExtra(INTENT_CHALLENGE_ID, 0)
            if (challengeId != 0) {
                challengeViewModel.viewChallenge(ChallengeCountRequest(challengeId))
            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@ChallengeDetailsActivity)
        locationPermission()

        listenToViewModel()
      intent?.let {
            if(it.getBooleanExtra(MainHomeActivity.INTENT_IS_SWITCH_ACCOUNT,false)) {
                intent?.getIntExtra(MainHomeActivity.USER_ID,0)?.let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        }
    }


    private fun listenToViewModel() {
        challengeViewModel.challengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeViewState.ChallengeLikeSuccess -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "challenge")
                    props.put(Constant.CONTENT_ID, it.challengeId)

                    mp?.track(Constant.SHARE_CONTENT, props)
                }
                is ChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChallengeViewState.ChallengeDetails -> {
                    if (it.challengeItem.id != null) {
                        challengeViewModel.challengeViewByUser(it.challengeItem.id.toString())
                    }
                    challengeItem = it.challengeItem
                    initUI(it.challengeItem)

                }
                is ChallengeViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    @SuppressLint("MissingPermission")
    private fun locationPermission() {
        XXPermissions.with(this).permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    locationPermissionGranted = all
                    val task = fusedLocationProviderClient.lastLocation
                    task.addOnSuccessListener { location ->
                        location?.let {
                            val lat = it.latitude
                            val long = it.longitude
                            currentLocation = "${lat}, $long"
                            Timber.tag("OkhttpClient").i("locationPermission : $currentLocation")
                            getAddress(lat, long)
                        }
                    }.addOnFailureListener { exception ->
                        exception.localizedMessage?.let {
                            showLongToast(it)
                        }
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    locationPermissionGranted = false
                }
            })
    }

    private fun getAddress(latitude: Double, longitude: Double) {
        try {
            val geo = Geocoder(this@ChallengeDetailsActivity, Locale.getDefault())
            val addresses = geo.getFromLocation(
                latitude, longitude, 1
            )
            city = addresses?.get(0)?.locality ?: ""
            state = addresses?.get(0)?.adminArea ?: ""
            country = addresses?.get(0)?.countryName ?: ""
        } catch (_: Exception) {
        }

    }

    private fun initUI(challengeItem: ChallengeItem) {
        challengeItem.userCity = city
        challengeItem.userState = state
        challengeItem.userCountry = country

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
        if (challengeItem.getIsFilePathImage()) {
            binding.jzvdStdOutgoer.isVisible = false
            binding.photoView.isVisible = true
            Glide.with(this@ChallengeDetailsActivity).load(challengeItem.filePath)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.photoView)
        } else {
            binding.jzvdStdOutgoer.isVisible = true
            binding.photoView.isVisible = false
            binding.jzvdStdOutgoer.videoUrl = challengeItem.filePath
            val player: JzvdStdOutgoer = binding.jzvdStdOutgoer
            player.apply {
                val jzDataSource = JZDataSource(this.videoUrl)
                jzDataSource.looping = true
                this.setUp(
                    jzDataSource, Jzvd.SCREEN_NORMAL, JZMediaExoKotlin::class.java
                )
                startVideoAfterPreloading()
            }
        }


        binding.tvUserName.text =
            if (!challengeItem.user?.userName.isNullOrEmpty() && challengeItem.user?.userName != "null") getString(
                R.string.sign_at_the_rate
            ).plus(
                challengeItem.user?.userName
            ) else getString(R.string.sign_at_the_rate).plus(
                challengeItem.user?.firstName
            ).plus("_").plus(
                challengeItem.user?.lastName
            )
        binding.tvName.text = if (!challengeItem.user?.userName.isNullOrEmpty() && challengeItem.user?.userName != "null")
            challengeItem.user?.userName else challengeItem.user?.firstName.plus(
            " "
        ).plus(challengeItem.user?.lastName)

        Glide.with(this@ChallengeDetailsActivity).load(challengeItem.user?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivUserProfileImage)
        binding.tvChallengeName.text = challengeItem.title
        binding.tvChallengeDescription.text = challengeItem.description
        binding.tvLikeCount.text = challengeItem.noOfLikesCount.toString()
        binding.tvWatchCount.text = challengeItem.noOfViewsCount.toString()
        binding.tvCommentCount.text = challengeItem.noOfCommentCount.toString()
        binding.tvCountry.text = challengeItem.challengeCountry?.firstOrNull()?.countryData?.name

        binding.profileRelativeLayout.visibility =
            if ((challengeItem.challengePostUser?.size ?: 0) > 0) View.VISIBLE else View.GONE


        if ((loggedInUserCache.getLoggedInUserId()) == challengeItem.user?.id ?: 0) {
            binding.llJoin.visibility = View.GONE
        } else {
            if (1 == (challengeItem.status ?: 0)) {
                if (challengeItem.challengeReactions != null) {
                    if ((challengeItem.challengeReactions.status ?: 0) != 0) {
                        binding.llJoin.visibility = View.GONE
                    } else {
                        if (challengeItem.challengeCity?.filter { it?.cityData?.name == "All" }
                                ?.isNotEmpty() == true) {
                            if (challengeItem.challengeState?.filter { it?.stateData?.name == "All" }
                                    ?.isNotEmpty() == true) {
                                if (challengeItem.challengeCountry?.filter { it?.countryData?.name == "All" }
                                        ?.isNotEmpty() == true) {
                                    binding.llJoin.visibility = View.VISIBLE
                                } else if (challengeItem.challengeCountry?.filter { it?.countryData?.name == challengeItem.userCountry }
                                        ?.isNotEmpty() == true) {
                                    binding.llJoin.visibility = View.VISIBLE
                                } else {
                                    binding.llJoin.visibility = View.GONE
                                }
                            } else if (challengeItem.challengeState?.filter { it?.stateData?.name == challengeItem.userState }
                                    ?.isNotEmpty() == true) {
                                binding.llJoin.visibility = View.VISIBLE
                            } else {
                                binding.llJoin.visibility = View.GONE
                            }
                        } else if (challengeItem.challengeCity?.filter { it?.cityData?.name == challengeItem.userCity }
                                ?.isNotEmpty() == true) {
                            binding.llJoin.visibility = View.VISIBLE
                        } else {
                            binding.llJoin.visibility = View.GONE
                        }
                    }
                } else {
                    if (challengeItem.challengeCity?.filter { it?.cityData?.name == "All" }
                            ?.isNotEmpty() == true) {
                        if (challengeItem.challengeState?.filter { it?.stateData?.name == "All" }
                                ?.isNotEmpty() == true) {
                            if (challengeItem.challengeCountry?.filter { it?.countryData?.name == "All" }
                                    ?.isNotEmpty() == true) {
                                binding.llJoin.visibility = View.VISIBLE
                            } else if (challengeItem.challengeCountry?.filter { it?.countryData?.name == challengeItem.userCountry }
                                    ?.isNotEmpty() == true) {
                                binding.llJoin.visibility = View.VISIBLE
                            } else {
                                binding.llJoin.visibility = View.GONE
                            }
                        } else if (challengeItem.challengeState?.filter { it?.stateData?.name == challengeItem.userState }
                                ?.isNotEmpty() == true) {
                            binding.llJoin.visibility = View.VISIBLE
                        } else {
                            binding.llJoin.visibility = View.GONE
                        }
                    } else if (challengeItem.challengeCity?.filter { it?.cityData?.name == challengeItem.userCity }
                            ?.isNotEmpty() == true) {
                        binding.llJoin.visibility = View.VISIBLE
                    } else {
                        binding.llJoin.visibility = View.GONE
                    }
                }
            } else {
                binding.llJoin.visibility = View.GONE
            }
        }

        if (challengeItem.status == 2 && challengeItem.winnerUser != null) {
            binding.ivWinnerUser.visibility = View.VISIBLE
            Glide.with(this@ChallengeDetailsActivity).load(challengeItem.winnerUser.profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivFirstUser)
        } else {
            binding.ivWinnerUser.visibility = View.INVISIBLE
        }
        challengeItem.challengePostUser?.reversed()?.let {
            if (it.size > 3) {
                binding.rlFirstContainer.visibility = View.VISIBLE
                binding.rlSecondContainer.visibility = View.VISIBLE
                binding.rlThirdContainer.visibility = View.VISIBLE
                binding.moreAppCompatTextView.visibility = View.VISIBLE

                Glide.with(this@ChallengeDetailsActivity).load(it[2].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivFirstUser)

                Glide.with(this@ChallengeDetailsActivity).load(it[1].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivSecondUser)

                Glide.with(this@ChallengeDetailsActivity).load(it[0].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivThirdUser)

                binding.moreAppCompatTextView.text = it.size.minus(3).toString()

            } else if (it.size > 2) {
                binding.rlFirstContainer.visibility = View.VISIBLE
                binding.rlSecondContainer.visibility = View.VISIBLE
                binding.rlThirdContainer.visibility = View.VISIBLE
                binding.moreAppCompatTextView.visibility = View.GONE

                Glide.with(this@ChallengeDetailsActivity).load(it[2].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivFirstUser)

                Glide.with(this@ChallengeDetailsActivity).load(it[1].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivSecondUser)

                Glide.with(this@ChallengeDetailsActivity).load(it[0].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivThirdUser)
            } else if (it.size > 1) {
                binding.rlFirstContainer.visibility = View.VISIBLE
                binding.rlSecondContainer.visibility = View.VISIBLE
                binding.rlThirdContainer.visibility = View.GONE
                binding.moreAppCompatTextView.visibility = View.GONE

                Glide.with(this@ChallengeDetailsActivity).load(it[1].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivFirstUser)

                Glide.with(this@ChallengeDetailsActivity).load(it[0].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivSecondUser)


            } else if (it.size > 0) {
                binding.rlFirstContainer.visibility = View.VISIBLE
                binding.rlSecondContainer.visibility = View.GONE
                binding.rlThirdContainer.visibility = View.GONE
                binding.moreAppCompatTextView.visibility = View.GONE

                Glide.with(this@ChallengeDetailsActivity).load(it[0].profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivFirstUser)
            } else {
                binding.rlFirstContainer.visibility = View.GONE
                binding.rlSecondContainer.visibility = View.GONE
                binding.rlThirdContainer.visibility = View.GONE
                binding.moreAppCompatTextView.visibility = View.GONE
            }
        }

        updateReelLike()
        binding.tvShortsTime.text = challengeItem.createdAt
        when (challengeItem.status) {
            1 -> {
                binding.llLive.isVisible = true
                binding.llUpcoming.isVisible = false
                binding.llComplete.isVisible = false
            }
            2 -> {
                binding.llLive.isVisible = false
                binding.llUpcoming.isVisible = false
                binding.llComplete.isVisible = true
            }
            else -> {
                binding.llLive.isVisible = false
                binding.llUpcoming.isVisible = true
                binding.llComplete.isVisible = false
            }
        }
        binding.likeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            updateLikeStatusCount()
        }.autoDispose()
        binding.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            if (challengeItem.user?.id != 0) {
                startActivity(
                    MyProfileActivity.getIntentWithData(
                        this@ChallengeDetailsActivity, challengeItem.user?.id ?: 0
                    )
                )
            }
        }.autoDispose()

        binding.ivComment.throttleClicks().subscribeAndObserveOnMainThread {
            if (challengeItem.id != null) {
                val challengeCommentBottomSheetFragment =
                    ChallengeCommentBottomSheetFragment.newInstance(
                        challengeItem.id,
                        challengeItem.user?.profilePhoto
                    )
                challengeCommentBottomSheetFragment.optionClick.subscribeAndObserveOnMainThread {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "challenge")
                    props.put(Constant.CONTENT_ID, challengeItem.id)

                    mp?.track(Constant.COMMENT_CONTENT, props)
                }
                challengeCommentBottomSheetFragment.show(
                    supportFragmentManager,
                    ChallengeCommentBottomSheetFragment::class.java.name
                )
            }
        }.autoDispose()
        binding.ivShare.throttleClicks().subscribeAndObserveOnMainThread {
            if (challengeItem.id != null) {
                ShareHelper.shareDeepLink(this@ChallengeDetailsActivity, 6, challengeItem.id, "",true) {

                    ShareHelper.shareText(this@ChallengeDetailsActivity, it)
                    showToast(resources.getString(R.string.msg_copied_video_link))

                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "challenge")
                    props.put(Constant.CONTENT_ID, challengeItem.id)

                    mp?.track(Constant.SHARE_CONTENT, props)
                }
            }
        }.autoDispose()

        binding.moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            var isRejectLiveJoin =
                if ((loggedInUserCache.getLoggedInUserId()) == challengeItem.user?.id ?: 0) {
                    false
                } else {
                    if (1 == (challengeItem.status ?: 0)) {
                        if (challengeItem.challengeReactions != null) {
                            if ((challengeItem.challengeReactions.status ?: 0) != 0) {
                                false
                            } else {
                                if (challengeItem.challengeCity?.filter { it?.cityData?.name == "All" }
                                        ?.isNotEmpty() == true) {
                                    if (challengeItem.challengeState?.filter { it?.stateData?.name == "All" }
                                            ?.isNotEmpty() == true) {
                                        if (challengeItem.challengeCountry?.filter { it?.countryData?.name == "All" }
                                                ?.isNotEmpty() == true) {
                                            true
                                        } else challengeItem.challengeCountry?.filter { it?.countryData?.name == challengeItem.userCountry }
                                            ?.isNotEmpty() == true
                                    } else challengeItem.challengeState?.filter { it?.stateData?.name == challengeItem.userState }
                                        ?.isNotEmpty() == true
                                } else challengeItem.challengeCity?.filter { it?.cityData?.name == challengeItem.userCity }
                                    ?.isNotEmpty() == true
                            }
                        } else {
                            if (challengeItem.challengeCity?.filter { it?.cityData?.name == "All" }
                                    ?.isNotEmpty() == true) {
                                if (challengeItem.challengeState?.filter { it?.stateData?.name == "All" }
                                        ?.isNotEmpty() == true) {
                                    if (challengeItem.challengeCountry?.filter { it?.countryData?.name == "All" }
                                            ?.isNotEmpty() == true) {
                                        true
                                    } else challengeItem.challengeCountry?.filter { it?.countryData?.name == challengeItem.userCountry }
                                        ?.isNotEmpty() == true
                                } else challengeItem.challengeState?.filter { it?.stateData?.name == challengeItem.userState }
                                    ?.isNotEmpty() == true
                            } else challengeItem.challengeCity?.filter { it?.cityData?.name == challengeItem.userCity }
                                ?.isNotEmpty() == true
                        }
                    } else {
                        false
                    }
                }

            openMoreOptionBottomSheet(
                challengeItem.id ?: 0,
                challengeItem.user?.id ?: 0,
                isRejectLiveJoin
            )
        }.autoDispose()

        binding.profileRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            if (challengeItem.id != 0) {
                val fragment = ChallengeReplayFragment.newInstance(challengeItem.id)
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.flContainer, fragment)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            }
        }.autoDispose()
        binding.replyAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(
                ChallengeReplyActivity.newInstance(
                    this@ChallengeDetailsActivity,
                    challengeId = challengeItem.id ?: 0
                )
            )
        }.autoDispose()


        binding.replyCountAppCompatTextView.visibility =
            if (challengeItem.challengePost?.size ?: 0 == 0) View.INVISIBLE else View.VISIBLE
        binding.replyCountAppCompatTextView.text =
            (challengeItem.challengePost?.size ?: 0).toString()

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

        val date1 = Date()
        val date2: Date = simpleDateFormat.parse(challengeItem.dateTo + " " + challengeItem.timeTo)

        when (challengeItem.status) {
            1 -> {
                val time = printDifference(date1, date2)
                if (time?.equals("Challenge Over", ignoreCase = true) == true) {
                    binding.itemLinearLayout.isVisible = false
                } else {
                    binding.itemAppCompatTextView.text = "Time left:"
                    binding.tvStartTime.text = time
                }
            }
            2 -> {

                binding.itemLinearLayout.isVisible = false
            }
            else -> {
                binding.itemLinearLayout.isVisible = true

                binding.tvStartTime.text =
                    "" + challengeItem.timeFrom + " " + UtilsClass.formatDate(
                        challengeItem.dateFrom
                    )
            }
        }
    }

    fun printDifference(startDate: Date, endDate: Date): String? {
        //milliseconds
        val difference_In_Time = endDate.time - startDate.time
        val difference_In_Seconds = ((difference_In_Time
                / 1000)
                % 60)
        val elapsedMinutes = ((difference_In_Time
                / (1000 * 60))
                % 60)
        val elapsedHours = ((difference_In_Time
                / (1000 * 60 * 60))
                % 24)
        val elapsedDays = ((difference_In_Time
                / (1000 * 60 * 60 * 24))
                % 365)
        return if (String.format("%d", elapsedDays).toInt() > 0) String.format(
            "%d",
            elapsedDays
        ) + " Day Left" else if (String.format("%d", elapsedHours).toInt() > 0) {
            String.format("%d", elapsedHours) + " Hour Left"
        } else if (String.format("%d", elapsedMinutes).toInt() > 0) {
            String.format("%d", elapsedMinutes) + " Minutes Left"
        } else if (String.format("%d", difference_In_Seconds).toInt() > 0) {
            String.format("%d", difference_In_Seconds) + " Seconds Left"
        } else "Challenge Over"
    }

    private fun openMoreOptionBottomSheet(
        challengeId: Int,
        challengeUserId: Int,
        isRejectLiveJoin: Boolean
    ) {
        val bottomSheet: MoreOptionBottomSheet = MoreOptionBottomSheet.newInstance(
            challengeId = challengeId,
            challengeUserId,
            isRejectLiveJoin
        )

        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.report) -> {
                    val reportDialog = ReportOptionBottomSheet.newInstance(challengeId, null)
                    reportDialog.optionClick.subscribeAndObserveOnMainThread {
                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "challenge")
                        props.put(Constant.CONTENT_ID, challengeId)

                        mp?.track(Constant.REPORT_CONTENT, props)
                    }
                    reportDialog.show(
                        supportFragmentManager,
                        ReportOptionBottomSheet::class.java.name
                    )
                }
                resources.getString(R.string.label_copy_link) -> {
                    ShareHelper.shareDeepLink(this@ChallengeDetailsActivity, 6, challengeId, "",true) {
                        val clipboard: ClipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Url", it)
                        clipboard.setPrimaryClip(clip)
                        showToast(resources.getString(R.string.msg_copied_video_link))
                    }
                }
            }
        }.autoDispose()
        bottomSheet.show(supportFragmentManager, MoreOptionBottomSheet::class.java.name)
    }

    private fun updateReelLike() {
        binding.apply {
            if (challengeItem.isLikeCount == 1) {
                likeAppCompatImageView.setImageResource(R.drawable.ic_fill_heart)
            } else {
                likeAppCompatImageView.setImageResource(R.drawable.ic_heart)
            }

            val totalLikes = challengeItem.noOfLikesCount
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    tvLikeCount.text = totalLikes.prettyCount().toString()
                    tvLikeCount.visibility = View.VISIBLE
                } else {
                    tvLikeCount.text = ""
                    tvLikeCount.visibility = View.GONE
                }
            } else {
                tvLikeCount.text = ""
                tvLikeCount.visibility = View.GONE
            }
        }
    }

    private fun updateLikeStatusCount() {
        if (challengeItem.isLikeCount == 0) {
            challengeItem.isLikeCount = 1
        } else {
            challengeItem.isLikeCount = 0
        }

        if (challengeItem.isLikeCount == 1) {
            challengeItem.noOfLikesCount = challengeItem.noOfLikesCount?.let { it + 1 } ?: 0
            updateReelLike()
            challengeViewModel.challengeLikeUnLike(ChallengeCountRequest(challengeId))
        } else {
            challengeItem.noOfLikesCount = challengeItem.noOfLikesCount?.let { it - 1 } ?: 0
            updateReelLike()
            challengeViewModel.challengeLikeUnLike(ChallengeCountRequest(challengeId))
        }
    }
}
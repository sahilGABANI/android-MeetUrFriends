package com.meetfriend.app.ui.challenge

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChallengeType
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentChallengeBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.utils.Constant
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class ChallengeFragment : BasicFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = ChallengeFragment()
    }

    private var _binding: FragmentChallengeBinding? = null
    private val binding get() = _binding!!


    private lateinit var challengeFragmentTabAdapter: ChallengeFragmentTabAdapter
    private var count = 0
    private var challengeType: ChallengeType = ChallengeType.AllChallenge

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        challengeFragmentTabAdapter = ChallengeFragmentTabAdapter(
            requireActivity()
        )
        binding.viewPager.isUserInputEnabled = false // Enable swipe gestures
        binding.viewPager.offscreenPageLimit = 4
        binding.viewPager.adapter = challengeFragmentTabAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.all)

                }
                1 -> {
                    tab.text = resources.getString(R.string.my_challenge)

                }
                2 -> {
                    tab.text = resources.getString(R.string.live)

                }
                3 -> {
                    tab.text = getString(R.string.completed)

                }
            }
        }.attach()
        binding.tabLayout.getTabAt(0)?.select()
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.tabLayout.invalidate()
                when (tab.text) {
                    resources.getString(R.string.all) -> {
                        challengeType = ChallengeType.AllChallenge
                        RxBus.publish(RxEvent.RefreshAllChallengeFragment(ChallengeType.AllChallenge))
                    }
                    resources.getString(R.string.my_challenge) -> {
                        challengeType = ChallengeType.MyChallenge
                        RxBus.publish(RxEvent.RefreshAllChallengeFragment(ChallengeType.MyChallenge))
                    }
                    resources.getString(R.string.live) -> {
                        challengeType = ChallengeType.LiveChallenge
                        RxBus.publish(RxEvent.RefreshAllChallengeFragment(ChallengeType.LiveChallenge))
                    }
                    resources.getString(R.string.completed) -> {
                        challengeType = ChallengeType.CompletedChallenge
                        RxBus.publish(RxEvent.RefreshAllChallengeFragment(ChallengeType.CompletedChallenge))
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }

        })
    }

    override fun onResume() {
        super.onResume()
        count += 1

        if (isResumed) {
            RxBus.publish(RxEvent.PlayChallengeVideo(true, count, challengeType))
            mp?.timeEvent(Constant.SCREEN_TIME)

            if (!loggedInUserCache.getLocationPermissionAsked()) {
                locationPermission()
            }
            if (isResumed && isVisible) {
                Timber.tag("RefreshAllChallenge").i("challengeType :$challengeType")
                RxBus.publish(RxEvent.RefreshAllChallengeFragment(challengeType))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.PlayChallengeVideo(false, count, challengeType))
    }

    override fun onStop() {
        super.onStop()
        val props = JSONObject()
        props.put(Constant.SCREEN_TYPE, "challenge")

        mp?.track(Constant.SCREEN_TIME, props)
    }
    private fun locationPermission() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionDialog()
            loggedInUserCache.setLocationPermissionAsked(true)
        }
    }

    private fun locationPermissionDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.label_location_permission))
        builder.setMessage(resources.getString(R.string.desc_why_location_permission_required))
        builder.setPositiveButton(
            resources.getString(R.string.label_yes)
        ) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            XXPermissions.with(this).permission(Permission.ACCESS_COARSE_LOCATION)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        Timber.tag("OkhttpClient").i("locationPermission : $permissions")
                    }
                })
        }

        builder.setNegativeButton(resources.getString(R.string.label_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
}
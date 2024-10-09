package com.meetfriend.app.ui.monetization

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.meetfriend.app.R
import com.meetfriend.app.api.monetization.model.SendHubRequestRequest
import com.meetfriend.app.api.monetization.model.SocialLinkInfo
import com.meetfriend.app.api.monetization.model.SocialType
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityHubRequestFormBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utilclasses.AppConstants
import com.meetfriend.app.utils.Constant
import contractorssmart.app.utilsclasses.CommonMethods
import java.text.SimpleDateFormat
import java.util.*


@Suppress("UNREACHABLE_CODE")
class HubRequestFormActivity : BasicActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HubRequestFormActivity::class.java)
        }
    }

    lateinit var binding: ActivityHubRequestFormBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val geocoder: Geocoder by lazy { Geocoder(this, Locale.getDefault()) }
    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHubRequestFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)

        listenToViewEvents()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

    }

    private fun listenToViewEvents() {
        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.edtDob.throttleClicks().subscribeAndObserveOnMainThread {
            openDatePicker()
        }.autoDispose()

        onSendButtonClick()

        RxBus.listen(RxEvent.FinishHubRequest::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -16)
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
            .listener(SingleDateAndTimePickerDialog.Listener { date: Date? ->
                var myCalendar = Calendar.getInstance()
                myCalendar.time = date
                if (calendar.time.before(myCalendar.time)) {
                    try {
                        CommonMethods.showToastMessageAtTop(
                            this,
                            getString(R.string.label_age_can_not_be_less_then_16_years)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // dob_string=myCalendar.timeInMillis.toString()
                    // val myFormat = "dd-MM-yyyy" //In which you need put here
                    try {
                        val sdf = SimpleDateFormat(AppConstants.CHALLENGE_DATE_FORMAT, Locale.US)
                        binding.edtDob.setText(sdf.format(myCalendar.time))
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            })
            .display()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val addresses: MutableList<Address>? = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )
                        if (addresses?.isNotEmpty() == true) {
                            val address: Address = addresses[0]
                            val street = address.thoroughfare ?: (address.featureName ?: "")
                            val city = address.locality ?: ""
                            val state = address.adminArea ?: ""
                            val postalCode = address.postalCode ?: ""

                            binding.edtAddress.setText("$street , $city , $state , $postalCode")
                            binding.ccp.setCountryForNameCode(address.countryCode)
                        }
                    }
                }
        }

    }
    // Function to handle click event of the "Done" button
    private fun onSendButtonClick() {
        binding.tvSendRequest.setOnClickListener {
            val validationError = validateInputFields()
            if (validationError != null) {
                showToast(validationError)
                return@setOnClickListener
            }

            val socialMediaLinks = ArrayList<SocialLinkInfo>()

            if (!validateAndAddSocialLink(binding.edtSocialLink.text.toString().trim(), SocialType.facebook, socialMediaLinks)) {
                return@setOnClickListener
            }
            if (!validateAndAddSocialLink(binding.edtSocialLinkTiktok.text.toString().trim(), SocialType.tiktok, socialMediaLinks)) {
                return@setOnClickListener
            }
            if (!validateAndAddSocialLink(binding.edtSocialLinkInsta.text.toString().trim(), SocialType.instagram, socialMediaLinks)) {
                return@setOnClickListener
            }
            if (!validateAndAddSocialLink(binding.edtSocialLinkTwitter.text.toString().trim(), SocialType.twitter, socialMediaLinks)) {
                return@setOnClickListener
            }

            // Only proceed if all links are valid
            val request = SendHubRequestRequest(
                binding.edtFirstName.text.toString(),
                binding.edtLstName.text.toString(),
                binding.edtDob.text.toString(),
                binding.edtAddress.text.toString(),
                binding.edtEmail.text.toString(),
                binding.edtPhoneNo.text.toString(),
                Gson().toJson(socialMediaLinks),
                countryCode = binding.ccp.selectedCountryCodeWithPlus
            )
            startActivity(SelectTaskActivity.getIntent(this, request))
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }
    }

    private fun validateInputFields(): String? {
        if (binding.edtFirstName.text.isNullOrEmpty()) {
            return getString(R.string.label_enter_first_name)
        }
        if (binding.edtLstName.text.isNullOrEmpty()) {
            return getString(R.string.label_enter_last_name)
        }
        if (binding.edtDob.text.isNullOrEmpty()) {
            return getString(R.string.label_select_dob)
        }
        if (binding.edtAddress.text.isNullOrEmpty()) {
            return getString(R.string.label_enter_address)
        }
        if (binding.edtEmail.text.isNullOrEmpty()) {
            return getString(R.string.label_enter_your_emailid)
        } else if (!CommonMethods.isValidEmail(binding.edtEmail.text.toString().trim())) {
            return getString(R.string.label_enter_valid_email)
        }
        if (binding.edtPhoneNo.text.isNullOrEmpty()) {
            return getString(R.string.label_enter_your_phoneno)
        }
        return null  // No validation error
    }

    private fun validateAndAddSocialLink(link: String, socialType: SocialType, socialMediaLinks: MutableList<SocialLinkInfo>, ): Boolean {
        if (link.isNotEmpty()) {
            if (isSocialLinkValid(socialType, link)) {
                socialMediaLinks.add(SocialLinkInfo(socialType.toString(), link))
                return true
            } else {
                showToast("Enter valid ${socialType.name} link")
                return false
            }
        }
        return true  // Allow empty links to be considered valid
    }

    private fun isSocialLinkValid(socialType: SocialType, link: String): Boolean {
        val regexPattern = getRegexPattern(socialType)
        val regex = Regex(regexPattern)
        return regex.matches(link)
    }

    private fun getRegexPattern(socialType: SocialType): String {
        return when (socialType) {
            SocialType.facebook -> "^https?://www.facebook.com/[a-zA-Z0-9.-_]+$"
            SocialType.tiktok -> "^https?://www.tiktok.com/@[a-zA-Z0-9._]+$"
            SocialType.instagram -> "(https?://)?(www\\.)?instagram\\.com/.*"
            SocialType.twitter -> "^https?://twitter.com/[a-zA-Z0-9_]+$"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
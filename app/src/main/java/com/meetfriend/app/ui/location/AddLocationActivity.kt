package com.meetfriend.app.ui.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.text.Editable
import android.text.TextWatcher
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.BuildConfig
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.places.PlaceSearch
import com.meetfriend.app.api.places.model.PlaceSearchResponse
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityAddLocationBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.location.view.LocationAdapter
import com.meetfriend.app.utils.UiUtils.hideKeyboard
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddLocationActivity : BasicActivity() {

    private lateinit var binding: ActivityAddLocationBinding
    private var currentLocation: String = ""

    private lateinit var locationAdapter: LocationAdapter

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        const val LOCATION_ADDRESS = "LOCATION_ADDRESS"
        var MAX_RADIUS = 2000
        const val SIXTY = 60L
        const val TWO_HANDRED_FOURTY = 240L
        const val THREE = 3
        fun getIntent(context: Context): Intent {
            return Intent(context, AddLocationActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermission()
        initUI()
    }

    private fun locationPermission() {
        XXPermissions.with(this)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                @SuppressLint("MissingPermission")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    val task = fusedLocationProviderClient.lastLocation
                    task.addOnSuccessListener { location ->
                        location?.let {
                            val lat = it.latitude
                            val longi = it.longitude
                            currentLocation = "$lat, $longi"
                        }
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    locationPermission()
                }
            })
    }

    private fun initUI() {
        locationAdapter = LocationAdapter(this).apply {
            locationItemClick.subscribeAndObserveOnMainThread {
                val intent = Intent()
                intent.putExtra(LOCATION_ADDRESS, it.formattedAddress)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.locationRecyclerView.apply {
            adapter = locationAdapter
        }

        binding.searchAppCompatEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if ((binding.searchAppCompatEditText.text.toString().length > THREE)) {
                    placeSearch(binding.searchAppCompatEditText.text.toString())
                } else {
                    locationAdapter.listOfDataItems = arrayListOf()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                Unit
        })
    }

    private fun placeSearch(search: String) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val okhttpBuilder = OkHttpClient.Builder()
            .readTimeout(SIXTY, TimeUnit.SECONDS)
            .connectTimeout(SIXTY, TimeUnit.SECONDS)
            .writeTimeout(TWO_HANDRED_FOURTY, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okhttpBuilder.addInterceptor(loggingInterceptor)
        }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okhttpBuilder.build())
            .build()

        val retrofitService = retrofit.create(PlaceSearch::class.java)

        loggedInUserCache.getPlaceApiKey()?.let {
            retrofitService.getPlacesLists(
                it,
                search,
                currentLocation,
                MAX_RADIUS
            )
                .enqueue(object :
                    Callback<PlaceSearchResponse> {
                    override fun onResponse(
                        call: Call<PlaceSearchResponse>,
                        response: Response<PlaceSearchResponse>
                    ) {
                        response.body()?.results?.let {
                            hideKeyboard(this@AddLocationActivity)
                            locationAdapter.listOfDataItems = it
                        }
                    }

                    override fun onFailure(call: Call<PlaceSearchResponse>, t: Throwable) = Unit
                })
        }
    }
}

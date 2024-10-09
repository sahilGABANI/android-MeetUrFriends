package com.meetfriend.app.ui.home.suggestion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.meetfriend.app.R
import com.meetfriend.app.databinding.ActivityUserSuggestionBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.follow.SuggestedFragment
import com.meetfriend.app.ui.main.MainHomeActivity
import contractorssmart.app.utilsclasses.PreferenceHandler

class UserSuggestionActivity : BasicActivity() {

    lateinit var binding: ActivityUserSuggestionBinding

    companion object {

        fun getIntent(context: Context): Intent {
            return Intent(context, UserSuggestionActivity::class.java)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSuggestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvent()

        if (savedInstanceState == null) { // initial transaction should be wrapped like this
            supportFragmentManager.beginTransaction()
                .replace(R.id.SuggestionFrameLayout, SuggestedFragment())
                .commitAllowingStateLoss()
        }


        (PreferenceHandler.writeString(
            this,
            PreferenceHandler.SHOW_SUGGESTION,
            "no"
        ))
    }

    private fun listenToViewEvent() {
        binding.skipAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(MainHomeActivity.getIntent(this))
            finish()
        }.autoDispose()
    }
}
package com.meetfriend.app.ui.tag

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import com.meetfriend.app.api.post.model.TaggedUser
import com.meetfriend.app.databinding.ActivityTaggedUserBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.ui.tag.view.TaggedUserAdapter

class TaggedUserActivity : BasicActivity() {

    companion object {
        private const val INTENT_TAGGED_USER_LIST = "INTENT_TAGGED_USER_LIST"

        fun getIntent(context: Context, taggedUserList: List<TaggedUser>): Intent {
            val intent = Intent(context, TaggedUserActivity::class.java)
            intent.putParcelableArrayListExtra(INTENT_TAGGED_USER_LIST, taggedUserList as ArrayList)
            return intent
        }
    }

    lateinit var binding: ActivityTaggedUserBinding

    private lateinit var taggedUserAdapter: TaggedUserAdapter
    private var taggedUserList: ArrayList<TaggedUser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTaggedUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            taggedUserList = it.getParcelableArrayListExtra(INTENT_TAGGED_USER_LIST)
        }

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        taggedUserAdapter = TaggedUserAdapter(this).apply {
            userClicks.subscribeAndObserveOnMainThread {
                startActivity(
                    MyProfileActivity.getIntentWithData(
                        this@TaggedUserActivity,
                        it.user.id
                    )
                )
            }.autoDispose()
        }
        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.recyclerViewSearchListing.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        binding.recyclerViewSearchListing.adapter = taggedUserAdapter
        taggedUserAdapter.listOfDataItems = taggedUserList as List<TaggedUser>

    }
}
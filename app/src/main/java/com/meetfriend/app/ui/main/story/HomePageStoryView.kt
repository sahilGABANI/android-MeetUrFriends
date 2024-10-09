package com.meetfriend.app.ui.main.story

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.StoryListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.storywork.models.HomePageStoryInfoState
import com.meetfriend.app.ui.storywork.models.ResultListResult
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class HomePageStoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val storyViewClickSubject: PublishSubject<HomePageStoryInfoState> =
        PublishSubject.create()
    val storyViewClick: Observable<HomePageStoryInfoState> = storyViewClickSubject.hide()

    private var binding: StoryListBinding? = null
    private lateinit var storyResponse: ArrayList<ResultListResult>
    private lateinit var storyAdapter: StoryAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.story_list, this)
        MeetFriend.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = StoryListBinding.bind(view)
    }

    fun bind(storyListResponse: ArrayList<ResultListResult>) {
        this.storyResponse = storyListResponse

        binding?.apply {
            storyAdapter = StoryAdapter(context).apply {
                storyViewClick.subscribeAndObserveOnMainThread {
                    storyViewClickSubject.onNext(it)
                }
            }

            storyRecyclerView.apply {
                adapter = storyAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                        val totalItemCount = layoutManager.itemCount
                        if (lastVisiblePosition == totalItemCount - 1 && !recyclerView.canScrollHorizontally(
                                1
                            )
                        ) {
                            storyViewClickSubject.onNext(HomePageStoryInfoState.LoadMoreStories)
                        }
                    }
                })
            }

            storyAdapter.listOfStories = storyResponse
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}

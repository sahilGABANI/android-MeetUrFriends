package com.meetfriend.app.ui.main.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.model.HomePagePostInfoState
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.ui.main.story.HomePageStoryView
import com.meetfriend.app.ui.storywork.models.HomePageStoryInfoState
import com.meetfriend.app.ui.storywork.models.ResultListResult
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MainHomeAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val addStoryViewClicksSubject: PublishSubject<String> = PublishSubject.create()
    val addStoryViewClicks: Observable<String> = addStoryViewClicksSubject.hide()

    private val storyViewClicksSubject: PublishSubject<HomePageStoryInfoState> = PublishSubject.create()
    val storyViewClicks: Observable<HomePageStoryInfoState> = storyViewClicksSubject.hide()

    private val postViewClicksSubject: PublishSubject<HomePagePostInfoState> =
        PublishSubject.create()
    val postViewClicks: Observable<HomePagePostInfoState> = postViewClicksSubject.hide()

    private val postViewByUserSubject: PublishSubject<Int> =
        PublishSubject.create()
    val postViewByUser: Observable<Int> = postViewByUserSubject.hide()

    private var adapterItems = listOf<AdapterItem>()
    var userId: Int = -1

    var listOfStory: ArrayList<ResultListResult>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    var listOfPosts: List<PostInformation>? = null
        set(listOfPosts) {
            field = listOfPosts
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfStory?.let {
            adapterItems.add(AdapterItem.StoriesViewItem(it))
        }

        listOfPosts?.forEach {
            adapterItems.add(AdapterItem.VideoPostsViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.StoriesViewItemType.ordinal -> {
                MediaAdapterViewHolder(
                    HomePageStoryView(context).apply {
                        addStoryViewClicks.subscribe { addStoryViewClicksSubject.onNext(it) }
                        storyViewClick.subscribe { storyViewClicksSubject.onNext(it) }
                    }
                )
            }

            ViewType.PostsVideoViewItemType.ordinal -> {
                MediaAdapterViewHolder(
                    MainVideoPostView(context).apply {
                        postViewClicks.subscribe {
                            postViewClicksSubject.onNext(it)
                        }
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems.get(position).type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.StoriesViewItem -> {
                (holder.itemView as HomePageStoryView).bind(adapterItem.storiesList)
            }

            is AdapterItem.VideoPostsViewItem -> {
                postViewByUserSubject.onNext(adapterItem.postsList.id)
                (holder.itemView as MainVideoPostView).bind(adapterItem.postsList, position)
            }
        }
    }

    private class MediaAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class StoriesViewItem(var storiesList: ArrayList<ResultListResult>) :
            AdapterItem(ViewType.StoriesViewItemType.ordinal)

        data class VideoPostsViewItem(var postsList: PostInformation) :
            AdapterItem(ViewType.PostsVideoViewItemType.ordinal)
    }

    private enum class ViewType {
        StoriesViewItemType,
        PostsVideoViewItemType
    }
}

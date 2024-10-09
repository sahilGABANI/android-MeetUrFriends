package com.meetfriend.app.ui.music.viewmodel

import com.meetfriend.app.api.music.MusicRepository
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MusicViewModel(
    private val musicRepository: MusicRepository
) : BasicViewModel() {

    private val musicStateSubject: PublishSubject<MusicInfoViewState> = PublishSubject.create()
    val musicState: Observable<MusicInfoViewState> = musicStateSubject.hide()

    private var listOfMusic: MutableList<MusicInfo> = mutableListOf()

    private var pageNumber: Int = 1
    private var perPage : Int = 20
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    fun resetPagination(search: String? = null) {
        listOfMusic.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        getMusicList(search)
    }

    fun loadMore(search: String? = null) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getMusicList(search)
            }
        }
    }
    private fun getMusicList(search: String? = null) {
        musicRepository.getMusicList(pageNumber, perPage, search ?: "latestsongs")
            .doOnSubscribe {
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(true))
            }
            .doAfterSuccess {
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({
                isLoading = false
                if(pageNumber == 1) {
                    listOfMusic.clear()
                }
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))
                if (it.data?.results?.isEmpty() == true) {
                    isLoadMore = false
                }
                it.data?.results?.let { musicList ->
                    listOfMusic.addAll(musicList)
                    musicStateSubject.onNext(MusicInfoViewState.GetMusicList(listOfMusic))
                }
            }, { throwable ->
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    musicStateSubject.onNext(MusicInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


}

sealed class MusicInfoViewState {
    data class LoadingState(val isLoading: Boolean) : MusicInfoViewState()
    data class ErrorMessage(val errorMessage: String) : MusicInfoViewState()
    data class GetMusicList(val listOfMusic: MutableList<MusicInfo>) : MusicInfoViewState()
}
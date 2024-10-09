package com.meetfriend.app.ui.challenge.viewmodel

import com.meetfriend.app.api.challenge.ChallengeRepository
import com.meetfriend.app.api.challenge.model.*
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.responseclasses.video.DataVideo
import com.meetfriend.app.ui.home.shorts.viewmodel.ShortsViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.RequestBody

class ChallengeViewModel(
    private val challengeRepository: ChallengeRepository, private val followRepository: FollowRepository
) : BasicViewModel() {


    private val challengeStateSubject: PublishSubject<ChallengeViewState> = PublishSubject.create()
    val challengeState: Observable<ChallengeViewState> = challengeStateSubject.hide()

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

    val perPage = 10
    private var pageNoH = 1
    private var isLoadingH = false
    private var isLoadMoreH = true

    private var listOfPostDataH: MutableList<ChallengeItem> = mutableListOf()

    fun getHashTagPost(hashTag: Int, searchString: String? = null) {
        challengeRepository.hashTagsChallenge(pageNoH, perPage, hashTag, searchString).doOnSubscribe {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
        }.doAfterTerminate {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            isLoadingH = false
        }.subscribeOnIoAndObserveOnMainThread({ it ->
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            if (it.status) {
                if (it.result != null) {
                    if (pageNoH == 1) {
                        listOfPostDataH.clear()
                        updatePostListH(it.result.data ?: listOf())
                    } else {
                        if (it.result.data?.isNotEmpty() == true) {
                            updatePostListH(it.result.data)
                        } else {
                            isLoadMoreH = false
                        }
                    }
                } else {
                    challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                    isLoadMoreH = false
                    updatePostListH(listOf())
                }
            }
        }, { throwable ->
            isLoadMoreH = false
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            challengeStateSubject.onNext(
                ChallengeViewState.ErrorMessage(
                    throwable.message ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private fun updatePostListH(listOfPostInformation: List<ChallengeItem>) {
        listOfPostDataH.addAll(listOfPostInformation)

        challengeStateSubject.onNext(
            ChallengeViewState.ListOfAllChallengeHashTag(
                listOfPostDataH
            )
        )
    }

    fun loadMorePostH(hashTag: Int) {
        if (!isLoadingH) {
            isLoadingH = true
            if (isLoadMoreH) {
                pageNoH++
                getHashTagPost(hashTag)
            }
        }
    }

    fun resetPaginationForHashTagPost(hashTag: Int, searchString: String? = null) {
        pageNoH = 1
        isLoadingH = false
        isLoadMoreH = true
        getHashTagPost(hashTag, searchString)
    }


    private var pageNoForLikedUser = 1
    private var isLoadingLikedUser = false
    private var isLoadMoreLikedUser = true

    private var listOfLikesChallenge: ArrayList<ChallengeUserModel> = arrayListOf()

    private fun getLikedUserChallengeList(challengeId: Int) {
        challengeRepository.challengeLikedUserList(
            page = pageNoForLikedUser,
            perPage = perPage,
            challengeId = challengeId
        ).doOnSubscribe {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            isLoadingLikedUser = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingLikedUser = false
            response?.let {
                if (response.status) {
                    if (pageNoForAllChallenge == 1) {
                        listOfLikesChallenge = response.result?.data ?: arrayListOf()
                        response.result?.let {
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfLikedUserInfo(
                                    listOfLikesChallenge
                                )
                            )
                        }
                    } else {
                        if (response.result != null) {
                            listOfLikesChallenge.addAll(response.result.data)
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfAllChallenge(
                                    listOfAllChallenge
                                )
                            )
                        } else {
                            isLoadMoreLikedUser = false
                        }
                    }
                } else {
                    response.message?.let {
                        challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(response.message))
                    }
                }
            }

        }, { throwable ->
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun resetPaginationLikedUserChallenge(challengeId: Int) {
        pageNoForAllChallenge = 1
        isLoadingForAllChallenge = false
        isLoadMoreForAllChallenge = true
        listOfAllChallenge.clear()
        getLikedUserChallengeList(challengeId)
    }


    private var pageNoForAllChallenge = 1
    private var isLoadingForAllChallenge = false
    private var isLoadMoreForAllChallenge = true

    private var listOfAllChallenge: MutableList<ChallengeItem> = mutableListOf()

    private fun getAllChallengeList() {
        challengeRepository.getChallengeList(
            page = pageNoForAllChallenge,
            perPage = perPage,
            isMyChallenge = 0,
            status = null
        ).doOnSubscribe {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            isLoadingForAllChallenge = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForAllChallenge = false
            response?.let {
                if (response.status) {
                    if (pageNoForAllChallenge == 1) {
                        if (!response.result.data.isNullOrEmpty()) {
                            response.result.data.toMutableList().let { listOfAllChallenge = it }
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfAllChallenge(
                                    listOfAllChallenge
                                )
                            )
                        } else {
                            challengeStateSubject.onNext(
                                ChallengeViewState.EmptyState(
                                    "no Challenge for Now"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfAllChallenge.addAll(response.result.data)
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfAllChallenge(
                                    listOfAllChallenge
                                )
                            )
                        } else {
                            isLoadMoreForAllChallenge = false
                        }
                    }
                } else {
                    response.message.let {
                        challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(response.message))
                    }
                }
            }

        }, { throwable ->
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreAllChallenge() {
        if (!isLoadingForAllChallenge) {
            isLoadingForAllChallenge = true
            if (isLoadMoreForAllChallenge) {
                pageNoForAllChallenge += 1

                getAllChallengeList()
            }
        }
    }

    fun resetPaginationForAllChallenge() {
        pageNoForAllChallenge = 1
        isLoadingForAllChallenge = false
        isLoadMoreForAllChallenge = true
        listOfAllChallenge.clear()
        getAllChallengeList()
    }

    private var pageNoForMyChallenge = 1
    private var isLoadingForMyChallenge = false
    private var isLoadMoreForMyChallenge = true
    private var listOfMyChallenge: MutableList<ChallengeItem> = mutableListOf()

    private fun getMyChallengeList() {
        challengeRepository.getChallengeList(
            page = pageNoForMyChallenge,
            perPage = perPage,
            isMyChallenge = 1,
            status = null
        ).doOnSubscribe {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            isLoadingForMyChallenge = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForMyChallenge = false
            response?.let {
                if (response.status) {
                    if (pageNoForMyChallenge == 1) {
                        if (response.result.data != null && response.result.data.isNotEmpty()) {
                            listOfMyChallenge = response.result.data.toMutableList()
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfMyChallenge(
                                    listOfMyChallenge
                                )
                            )
                        } else {
                            challengeStateSubject.onNext(
                                ChallengeViewState.EmptyState(
                                    "no Challenge for You"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfMyChallenge.addAll(response.result.data)
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfMyChallenge(
                                    listOfMyChallenge
                                )
                            )
                        } else {
                            isLoadMoreForMyChallenge = false
                        }
                    }
                } else {
                    response.message.let {
                        challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(response.message))
                    }
                }
            }

        }, { throwable ->
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreMyChallenge() {
        if (!isLoadingForMyChallenge) {
            isLoadingForMyChallenge = true
            if (isLoadMoreForMyChallenge) {
                pageNoForMyChallenge += 1
                getMyChallengeList()
            }
        }
    }

    fun resetPaginationForMyChallenge() {
        pageNoForMyChallenge = 1
        isLoadingForMyChallenge = false
        isLoadMoreForMyChallenge = true
        listOfMyChallenge.clear()
        getMyChallengeList()
    }

    private var pageNoForLiveChallenge = 1
    private var isLoadingForLiveChallenge = false
    private var isLoadMoreForLiveChallenge = true
    private var listOfLiveChallenge: MutableList<ChallengeItem> = mutableListOf()

    private fun getLiveChallengeList() {
        challengeRepository.getChallengeList(
            page = pageNoForLiveChallenge,
            perPage = perPage,
            isMyChallenge = 0,
            status = 1
        ).doOnSubscribe {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            isLoadingForLiveChallenge = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForLiveChallenge = false
            response?.let {
                if (response.status) {
                    if (pageNoForLiveChallenge == 1) {
                        if (response.result.data != null && response.result.data.isNotEmpty()) {
                            listOfLiveChallenge = response.result.data.toMutableList()
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfLiveChallenge(
                                    listOfLiveChallenge
                                )
                            )
                        } else {
                            challengeStateSubject.onNext(
                                ChallengeViewState.EmptyState(
                                    "no Challenge for You"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfLiveChallenge.addAll(response.result.data)
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfLiveChallenge(
                                    listOfLiveChallenge
                                )
                            )
                        } else {
                            isLoadMoreForLiveChallenge = false
                        }
                    }
                } else {
                    response.message.let {
                        challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(response.message))
                    }
                }
            }

        }, { throwable ->
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreLiveChallenge() {
        if (!isLoadingForLiveChallenge) {
            isLoadingForLiveChallenge = true
            if (isLoadMoreForLiveChallenge) {
                pageNoForLiveChallenge += 1
                getLiveChallengeList()
            }
        }
    }

    fun resetPaginationForLiveChallenge() {
        pageNoForLiveChallenge = 1
        isLoadingForLiveChallenge = false
        isLoadMoreForLiveChallenge = true
        listOfLiveChallenge.clear()
        getLiveChallengeList()
    }

    private var pageNoForCompleteChallenge = 1
    private var isLoadingForCompleteChallenge = false
    private var isLoadMoreForCompleteChallenge = true
    private var listOfCompleteChallenge: MutableList<ChallengeItem> = mutableListOf()

    private fun getCompleteChallengeList() {
        challengeRepository.getChallengeList(
            page = pageNoForCompleteChallenge,
            perPage = perPage,
            isMyChallenge = 0,
            status = 2
        ).doOnSubscribe {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            isLoadingForCompleteChallenge = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForCompleteChallenge = false
            response?.let {
                if (response.status) {
                    if (pageNoForCompleteChallenge == 1) {
                        if (response.result.data != null && response.result.data.isNotEmpty()) {
                            listOfCompleteChallenge = response.result.data.toMutableList()
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfCompleteChallenge(
                                    listOfCompleteChallenge
                                )
                            )
                        } else {
                            challengeStateSubject.onNext(
                                ChallengeViewState.EmptyState(
                                    "no Challenge for You"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfCompleteChallenge.addAll(response.result.data)
                            challengeStateSubject.onNext(
                                ChallengeViewState.ListOfCompleteChallenge(
                                    listOfCompleteChallenge
                                )
                            )
                        } else {
                            isLoadMoreForCompleteChallenge = false
                        }
                    }
                } else {
                    response.message.let {
                        challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(response.message))
                    }
                }
            }

        }, { throwable ->
            challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreCompleteChallenge() {
        if (!isLoadingForCompleteChallenge) {
            isLoadingForCompleteChallenge = true
            if (isLoadMoreForCompleteChallenge) {
                pageNoForCompleteChallenge += 1
                getCompleteChallengeList()
            }
        }
    }

    fun resetPaginationForCompleteChallenge() {
        pageNoForCompleteChallenge = 1
        isLoadingForCompleteChallenge = false
        isLoadMoreForCompleteChallenge = true
        listOfCompleteChallenge.clear()
        getCompleteChallengeList()
    }


    fun viewChallenge(challengeCountRequest: ChallengeCountRequest) {
        challengeRepository.challengeView(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                challengeStateSubject.onNext(ChallengeViewState.ChallengeDetails(response.result))
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun challengeLikeUnLike(challengeCountRequest: ChallengeCountRequest) {
        challengeRepository.challengeLikeUnLike(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                challengeStateSubject.onNext(
                    ChallengeViewState.ChallengeLikeSuccess(
                        challengeCountRequest.challengeId
                    )
                )
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun challengeViewByUser(challengeId: String) {
        challengeRepository.challengeViewByUser(challengeId = challengeId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    challengeStateSubject.onNext(ChallengeViewState.SuccessViewMessage(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun reportChallenge(reportChallengeRequest: ReportChallengeRequest) {
        challengeRepository.reportChallenge(reportChallengeRequest = reportChallengeRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    challengeStateSubject.onNext(ChallengeViewState.SuccessMessage(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }


    fun deleteChallenge(challengeCountRequest: ChallengeCountRequest) {
        challengeRepository.deleteChallenge(challengeCountRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    challengeStateSubject.onNext(ChallengeViewState.SuccessDeleteChallengeMessage(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteChallengePost(challengeCountRequest: DeleteChallengePostRequest) {
        challengeRepository.deleteChallengePost(challengeCountRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    challengeStateSubject.onNext(ChallengeViewState.SuccessDeleteChallengeMessage(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadChallengeAcceptFile(file: RequestBody, challengeId: Int) {
        challengeRepository.uploadChallengeAcceptFile(file)
            .doOnSubscribe {
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(true))
            }.doAfterSuccess {
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                isLoadingForCompleteChallenge = false
            }
            .subscribeOnIoAndObserveOnMainThread({

                challengeRepository.challengeAcceptRejectPost(
                    ChallengeReactions(
                        challengeId,
                        status = 1
                    )
                )
                    .subscribeOnIoAndObserveOnMainThread({ response ->
                        response.message?.let {
                            challengeStateSubject.onNext(
                                ChallengeViewState.SuccessCreateChallengeMessage(
                                    it
                                )
                            )
                        }
                    }, { throwable ->
                        challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                        throwable.localizedMessage?.let {
                            challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                        }
                    }).autoDispose()

            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun challengeDetail(challengeCountRequest: ChallengeCountRequest) {
        challengeRepository.challengeDetail(challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->

                response.result.let {
                    challengeStateSubject.onNext(ChallengeViewState.ChallengeDetails(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun challengePostLikeUnLike(challengePostLikeRequest: ChallengePostLikeRequest) {
        challengeRepository.challengePostLikeUnlike(challengePostLikeRequest = challengePostLikeRequest)
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                challengeStateSubject.onNext(
                    ChallengeViewState.ChallengeLikeSuccess(
                        challengePostLikeRequest.challengeId
                    )
                )
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }


    fun reportChallengePost(reportChallengeRequest: ReportChallengePostRequest) {
        challengeRepository.reportChallengePost(reportChallengeRequest = reportChallengeRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    challengeStateSubject.onNext(ChallengeViewState.SuccessMessage(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun challengePostViewByUser(challengePostViewByUserRequest: ChallengePostViewByUserRequest) {
        challengeRepository.challengePostViewByUser(challengePostViewByUserRequest = challengePostViewByUserRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    challengeStateSubject.onNext(ChallengeViewState.SuccessViewMessage(it))
                }
            }, { throwable ->
                challengeStateSubject.onNext(ChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeStateSubject.onNext(ChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }
}

sealed class ChallengeViewState {
    data class ErrorMessage(val errorMessage: String) : ChallengeViewState()
    data class LoadingState(val isLoading: Boolean) : ChallengeViewState()
    data class ChallengeLikeSuccess(val challengeId: Int) : ChallengeViewState()
    data class EmptyState(val message: String) : ChallengeViewState()
    data class SuccessMessage(val successMessage: String) : ChallengeViewState()
    data class ChallengeDetails(val challengeItem: ChallengeItem) : ChallengeViewState()
    data class SuccessDeleteChallengeMessage(val successMessage: String) : ChallengeViewState()
    data class SuccessCreateChallengeMessage(val successMessage: String) : ChallengeViewState()
    data class SuccessViewMessage(val successMessage: String) : ChallengeViewState()
    data class UpdateCommentMessage(val challengeItem: ChallengeComment) : ChallengeViewState()
    data class ListOfLikedUserInfo(val listOfAllChallenge: List<ChallengeUserModel>) :
        ChallengeViewState()

    data class ListOfAllChallenge(val listOfAllChallenge: List<ChallengeItem>) :
        ChallengeViewState()

    data class ListOfAllChallengeHashTag(val listOfAllChallenge: List<ChallengeItem>) :
        ChallengeViewState()

    data class ListOfMyChallenge(val listOfAllChallenge: List<ChallengeItem>) : ChallengeViewState()
    data class ListOfLiveChallenge(val listOfAllChallenge: List<ChallengeItem>) :
        ChallengeViewState()

    data class ListOfCompleteChallenge(val listOfAllChallenge: List<ChallengeItem>) :
        ChallengeViewState()
}
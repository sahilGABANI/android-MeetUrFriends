package com.meetfriend.app.api.music

import com.meetfriend.app.api.music.model.MusicResponse
import io.reactivex.Single

class MusicRepository(private val musicRetrofitAPI: MusicRetrofitAPI) {

    fun getMusicList(page: Int, limit: Int, search: String? = null): Single<MusicResponse> {
        return musicRetrofitAPI.getMusicList(page, limit, search)
    }
}

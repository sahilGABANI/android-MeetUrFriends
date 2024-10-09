package com.meetfriend.app.network;

import com.meetfriend.app.responseclasses.CommonDataModel;
import com.meetfriend.app.responseclasses.CommonResponseClass;
import com.meetfriend.app.utilclasses.Util;

import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

public class API {
    public static Call<CommonResponseClass> opentokSession(String friend_id,String call_type,String type) {
        return Util.requestApiDefault().opentokSession("api/chat/get-token?friend_id="+friend_id+"&call_type="+call_type+"&type="+type);
    }
    public static Call<CommonDataModel> sendNoti(String friend_id, String message, String gift_id, String type) {
        HashMap<String, RequestBody> map = new HashMap();
        map.put("friend_id", RequestBody.create(MultipartBody.FORM, friend_id));
        map.put("message", RequestBody.create(MultipartBody.FORM, message));
        map.put("gift_id", RequestBody.create(MultipartBody.FORM, gift_id));
        map.put("type", RequestBody.create(MultipartBody.FORM, type));
        return Util.requestApiDefault().sendRequest( "api/chat/send-notification", map);
    }
}
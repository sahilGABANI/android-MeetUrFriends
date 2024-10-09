package com.meetfriend.app.network;

import android.content.Context;

import com.meetfriend.app.utilclasses.CallProgressWheel;
import com.meetfriend.app.utilclasses.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public abstract class ServerRequest<T> {

    private final Context mContext;
    private final Call<T> call;


    public ServerRequest(final Context mContext, Call<T> call, boolean showProgress) {
        this.mContext = mContext;
        this.call = call;
        if (showProgress) {
            CallProgressWheel.INSTANCE.showLoadingDialog(mContext);

        }

        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {

                try {
                    CallProgressWheel.INSTANCE.dismissLoadingDialog();

                    //   }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response.isSuccessful())
                    afterResponse(call, response);
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                try {

                    ////}
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Util.showLog("Exp : " + t.getMessage());
                Util.showMessageDialog(mContext,"MeetUrFriends", "Server not responding. Please try after sometime");
            }
        });

    }


    private void afterResponse(Call<T> call, Response<T> response) {
        onCompletion(call, response);
    }

    public abstract void onCompletion(Call<T> call, Response<T> response);

}

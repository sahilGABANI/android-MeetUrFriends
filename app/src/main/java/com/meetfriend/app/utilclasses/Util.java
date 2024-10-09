package com.meetfriend.app.utilclasses;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.meetfriend.app.application.MeetFriendApplication;
import com.meetfriend.app.network.ApiInterface;

import java.util.concurrent.TimeUnit;

import contractorssmart.app.utilsclasses.PreferenceHandler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Util {

    /**
     * Show alert dialog
     *
     * @param mContext         Context of activity o fragment
     * @param message          Message that shows on Dialog
     * @param title            Title for dialog
     * @param positiveText     Set positive text
     * @param positiveListener Set functionality on positive button click
     * @param negativeListener Set functionality on negative button click
     * @param negativeText     Negative button text
     * @param neutralText      Neturat button text
     * @param neutralListener  Set Netural button listener
     * @param isCancelable     true -> Cancelable True ,false -> Cancelable False
     * @return dialog
     */
    public static AlertDialog.Builder showDialog(Context mContext, String message, String title, String positiveText, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, String negativeText, String neutralText, DialogInterface.OnClickListener neutralListener, boolean isCancelable) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setNegativeButton(negativeText, negativeListener);
        alert.setPositiveButton(positiveText, positiveListener);
        alert.setNeutralButton(neutralText, neutralListener);
        alert.setCancelable(isCancelable);
        alert.show();
        return alert;
    }

    public static AlertDialog.Builder showMessageDialog(Context mContext, String title, String message) {
        return showDialog(mContext, message, title, "OK", null, null, null, null, null, true);
    }

    public static ApiInterface requestApiDefault() {
        String  token = PreferenceHandler.INSTANCE.readString(
                MeetFriendApplication.context.getApplicationContext(),
               "AUTHORIZATION_TOKEN",
                ""
        );
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectTimeout(1000, TimeUnit.SECONDS)
                .writeTimeout(1000, TimeUnit.SECONDS)

                .readTimeout(1000, TimeUnit.SECONDS);
        clientBuilder.addInterceptor(chain -> {
            Request request;
            {

                request = chain.request().newBuilder()

               . addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();
            }
            return chain.proceed(request);
        });

        clientBuilder.addInterceptor(logging);

        OkHttpClient client = clientBuilder.build();

        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(AppConstants.INSTANCE.getAPIS_BASE_URL()).client(client).addConverterFactory(GsonConverterFactory.create(gson)).build();

        ApiInterface apis = retrofit.create(ApiInterface.class);
        return apis;
    }


    public static void intentToActivityWithBundle(Context mContext, Class activityToOpen, Bundle bundle, boolean clearStack) {
        Intent intent = new Intent(mContext, activityToOpen);
        if (clearStack)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(bundle);
        mContext.startActivity(intent);
    }

    /**
     * Show Log
     *
     * @param message Message that want to show into Log
     */
    public static void showLog(String message) {
    }

}
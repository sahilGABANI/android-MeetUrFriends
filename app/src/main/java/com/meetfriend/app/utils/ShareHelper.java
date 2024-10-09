package com.meetfriend.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.meetfriend.app.BuildConfig;
import com.meetfriend.app.R;

import timber.log.Timber;

public class ShareHelper {

    private final static String TAG = "ShareHelper";

    private final static String baseUrl = "https://meeturfriends.com/";
    private final static String fallBackUrl = "https://play.google.com/store/apps/details?id=com.meetfriend.app";
    private final static String iosBundleId = "com.app.meet-friends";

    public static void shareText(Context context, String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        context.startActivity(sendIntent);
    }

    public static void shareDeepLink(Context context, int shareType, int postOrReelId, String imageUrl, boolean showToast ,SetOnShareUrl setOnShareUrl) {
        if(showToast) {
            Toast.makeText(context, context.getResources().getString(R.string.msg_preparing_link_wait_for_moment), Toast.LENGTH_LONG).show();
        }
        String deepLinkUrl;

        switch(shareType) {
            case 0:
                deepLinkUrl = baseUrl + "post/" + postOrReelId;
                break;
            case 1:
                deepLinkUrl = baseUrl + "reels/" + postOrReelId;
                break;
            case 2:
                deepLinkUrl = baseUrl + "user/" + postOrReelId;
                break;
            case 3:
                deepLinkUrl = baseUrl + "live/" + postOrReelId;
                break;
            case 4:
                deepLinkUrl = baseUrl + "images_share/" + imageUrl;
                break;
            case 5:
                deepLinkUrl = baseUrl + "chat_profile/" + postOrReelId;
                break;
            case 6:
                deepLinkUrl = baseUrl + "challenge/" + postOrReelId;
                break;
            default:
                deepLinkUrl = null;
        }

        FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLongLink(Uri.parse(baseUrl + "?link=" + deepLinkUrl + "&apn=" + BuildConfig.APPLICATION_ID + "&ibn=" + iosBundleId + "&afl=" + fallBackUrl))
                .buildShortDynamicLink()
                .addOnCompleteListener((Activity) context, new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {
                            // Short link created
                            Uri shortLink = task.getResult().getShortLink();
                            Uri flowchartLink = task.getResult().getPreviewLink();
                            if (shortLink != null){
                                setOnShareUrl.loadShareUrl(shortLink.toString());
                                Timber.tag(TAG).i("flowchartLink: %s", shortLink.toString());
                            }
                            if (flowchartLink != null)
                                Timber.tag(TAG).i("flowchartLink: %s", flowchartLink.toString());
                        } else {
                            Timber.tag(TAG).e("Fail to create deeplink");
                        }
                    }
                });
    }

    public interface SetOnShareUrl {
        void loadShareUrl(String shareUrl);
    }
}
package com.meetfriend.app.utilclasses;

import static androidx.core.app.NotificationCompat.DEFAULT_SOUND;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;
import static com.google.firebase.messaging.RemoteMessage.PRIORITY_HIGH;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.meetfriend.app.R;
import com.meetfriend.app.api.authentication.LoggedInUserCache;
import com.meetfriend.app.api.chat.model.ChatRoomInfo;
import com.meetfriend.app.api.chat.model.ChatRoomUser;
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo;
import com.meetfriend.app.application.MeetFriendApplication;
import com.meetfriend.app.newbase.ActivityManager;
import com.meetfriend.app.newbase.RxBus;
import com.meetfriend.app.newbase.RxEvent;
import com.meetfriend.app.ui.activities.CallLauncherActivity;
import com.meetfriend.app.ui.activities.MainActivity;
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity;
import com.meetfriend.app.ui.chatRoom.videoCall.OneToOneVideoCallActivity;
import com.meetfriend.app.ui.main.MainHomeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import javax.inject.Inject;

import contractorssmart.app.utilsclasses.PreferenceHandler;

public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = "MyFcmListenerService";
    Intent resultIntent;

    @Inject
    LoggedInUserCache loggedInUserCache;

    @Override
    public void onCreate() {
        super.onCreate();
        MeetFriendApplication.component.inject(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
    }

    @Override
    public void handleIntent(Intent intent) {
        super.handleIntent(intent);

        Activity foregroundActivity = ActivityManager.Companion.getInstance().getForegroundActivity();

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);

        if (myKM.inKeyguardRestrictedInputMode()) {
            //it is locked
            Bundle bundle = intent.getExtras();
            String map1 = bundle.getString("extra_data");
            String title = bundle.getString("title");
            String body = bundle.getString("body");


            if (map1 != null) {
                JSONObject map2 = null;
                try {
                    map2 = new JSONObject(map1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String typeL = map2.optString("type");
                String typeId = map2.optString("type_id");

                if (typeL.equals("host_invite")) {
                    Intent mIntent = new Intent(this, MainActivity.class);
                    mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mIntent.putExtras(bundle);

                    String map = bundle.getString("message");
                    JSONObject map3 = null;
                    try {
                        if (map != null) {
                            map3 = new JSONObject(map);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    GsonBuilder gsonbuild = new GsonBuilder();
                    gsonbuild.setPrettyPrinting();

                    Gson gson = gsonbuild.create();
                    LiveEventInfo liveEventInfo = gson.fromJson(map, LiveEventInfo.class);
                    liveEventInfo.setCoHost(liveEventInfo.isCoHostNotification());
                    liveEventInfo.setHostStatus(liveEventInfo.getHostStatusNotification());

                    Boolean isLoginUser = true ;

                    if(map2.optInt("to_user") != 0) {
                        if(loggedInUserCache.getLoggedInUser().getLoggedInUser().getUserId() == map2.optInt("to_user")) {
                            isLoginUser = true;
                        } else {
                            isLoginUser = false;
                        }
                    }
                    Intent notifyIntent =
                            MainHomeActivity.Companion.launchFromLiveCoHostNotification(this, Integer.parseInt(typeId), false,isLoginUser,map2.optInt("to_user"));


                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            this, 0, notifyIntent,
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );


                    if (map3 != null) {
                        Notification.Builder mBuilder =
                                new Notification.Builder(this)
                                        .setSmallIcon(R.drawable.ic_meet_friend_logo_white)
                                        .setContentTitle(map3.optString("title"))
                                        .setContentText(map3.optString("body"))
                                        .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE) //Important for heads-up notification
                                        .setContentIntent(pendingIntent)
                                        .setGroup("defaultGroup")
                                        .setPriority(Notification.PRIORITY_HIGH)
                                        .setAutoCancel(true)
                                        .setCategory(NotificationCompat.CATEGORY_CALL);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mBuilder.setChannelId("124562345467446589");
                        }
                        notificationManager.notify(new Random().nextInt(), mBuilder.build());
                    }
                } else if (typeL.equals("chat")) {
                    String msgType = map2.optString("message_type");

                    if (body != null && body.contains("requested you to join chat")) {
                        generateChatNotification(this, getString(R.string.application_name), body, null,map2.optInt("receiver_id"));
                    } else if (msgType != null) {

                        ChatRoomUser receiver = new ChatRoomUser((map2.optInt("sender_id")), map2.optString("sender_name"), null, map2.optString("sender_profile"), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,map2.optInt("is_verified"));
                        ChatRoomInfo chatRoomInfo = new ChatRoomInfo(null, true, map2.optString("created_at"), map2.optString("message"), map2.optString("message_type"), null, null, null, map2.optString("created_at"), map2.optInt("sender_id"), null, map2.optInt("conversation_id"), map2.optInt("receiver_id"), null, (map2.optInt("room_type")), null, null, null, null, null, null, null, null, false, null, null, null, null, null, receiver, null, 0, 1,map2.optInt("is_verified"), false);

                        if (body == null) {
                            body = msgType;
                        }

                        generateChatNotification(this, title, body, chatRoomInfo,map2.optInt("receiver_id"));
                    }
                } else {
                    String map = bundle.getString("message");
                    JSONObject map3 = null;
                    try {
                        map3 = new JSONObject(map);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map3.optString("body").equalsIgnoreCase("calling call notification")) {
                        try {
                            if (map2.getString("type").equalsIgnoreCase("missed")) {
                                if (PreferenceHandler.INSTANCE.readString(getApplicationContext(), "isOpen", "0").equalsIgnoreCase("1")) {
                                    PreferenceHandler.INSTANCE.writeString(getApplicationContext(), "isOpen", "0");
                                    new CallLauncherActivity().runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            CallLauncherActivity.binding.mBack.performClick();
                                        }
                                    });
                                }
                                if (map2.getString("call_type").equalsIgnoreCase("audio")) {
                                    generateNotification(this, "Missed call", "You missed a voice call", "", "", 0,0);

                                } else {
                                    generateNotification(this, "Missed call", "You missed a video call", "", "", 0,0);
                                }
                            } else if (map2.getString("type").equalsIgnoreCase("disconnect")) {
                                if (map2.getString("call_type").equalsIgnoreCase("audio")) {
                                } else {
                                }
                            } else if (map2.getString("call_type").equalsIgnoreCase("audio")) {
                                AudioNotification(map2);
                            } else if (map2.getString("type").equalsIgnoreCase("calling")) {
                                if (map2.getString("call_type").equalsIgnoreCase("video")) {
                                    oneToOneVideoNotification(map2, 1);
                                }
                            } else oneToOneVideoNotification(map2, 0);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (map3.optString("body").equalsIgnoreCase("call end notification")) {

                    } else if (map3.optString("body").equalsIgnoreCase("missed call notification")) {
                        try {
                            generateNotification(this, "Missed call", "You missed a video call from " + (map2.getString("from_user_name")), "", "", 0,0);
                        } catch (JSONException e) {
                            generateNotification(this, "Missed call", "You missed a video call", "", "", 0,0);
                        }

                    } else {
                        generateNotification(this, map3.optString("title"), map3.optString("body"), typeL, typeId, map3.optInt("from_user"), map3.optInt("to_user"));
                    }
                }
            }

        } else {
            //it is not locked
            Bundle bundle = intent.getExtras();
            String map1 = bundle.getString("extra_data");
            String title = bundle.getString("title");
            String body = bundle.getString("body");

            if (map1 != null) {
                JSONObject map2 = null;
                try {
                    map2 = new JSONObject(map1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String type = map2.optString("type");
                if (type.equals("host_invite")) {
                    String map = bundle.getString("extra_data");
                    String maps1 = bundle.getString("message");
                    JSONObject map3 = null;
                    JSONObject maps2 = null;
                    try {
                        if (maps1 != null) {
                            maps2 = new JSONObject(maps1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (map != null) {
                            map3 = new JSONObject(map);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    GsonBuilder gsonbuild = new GsonBuilder();
                    gsonbuild.setPrettyPrinting();

                    Gson gson = gsonbuild.create();
                    LiveEventInfo liveEventInfo = gson.fromJson(map, LiveEventInfo.class);
                    liveEventInfo.setId(map3.optInt("type_id", 0));
                    liveEventInfo.setCoHost(liveEventInfo.isCoHostNotification());
                    liveEventInfo.setHostStatus(liveEventInfo.getHostStatusNotification());
                    liveEventInfo.setChannelId(map3.optString("channel_id"));
                    liveEventInfo.setToken(map3.optString("token"));
                    liveEventInfo.setName(map3.optString("event_name"));
                    liveEventInfo.setRoleType(map3.optString("role_type"));
                    liveEventInfo.setProfilePhoto(map3.optString("profile_photo"));
                    liveEventInfo.setUserName(map3.optString("userName"));
                    liveEventInfo.setFollowingStatus(map3.optInt("following_status"));
                    liveEventInfo.setCoHost(map3.optInt("is_co_host"));
                    liveEventInfo.setHostStatus(map3.optInt("host_status"));
                    liveEventInfo.setNoOfFollowers(map3.optInt("no_of_followers"));
                    liveEventInfo.setAllowPlayGame(map3.optInt("is_allow_play_game"));


                    loggedInUserCache.invitedAsCoHost(liveEventInfo);


                    Boolean isLoginUser = true ;
                    if(map3.optInt("to_user") != 0) {
                        if(loggedInUserCache.getLoggedInUser().getLoggedInUser().getId() == map3.optInt("to_user")) {
                            isLoginUser = true;
                        } else {
                            isLoginUser = false;
                        }
                    }
                    Boolean isJoin = false;
                    if (foregroundActivity != null) {
                        isJoin = foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.chatRoom.videoCall.OneToOneVideoCallActivity");
                    }
                    Intent notifyIntent =
                            MainHomeActivity.Companion.launchFromLiveCoHostNotification(this, Integer.parseInt(map3.optString("type_id")), isJoin,isLoginUser,map3.optInt("to_user"));

                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            this, 0, notifyIntent,
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );

                    if (foregroundActivity != null) {
                        if (foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.livestreaming.watchlive.WatchLiveStreamingActivity")) {
                            RxBus.INSTANCE.publish(RxEvent.Companion.coHostRequestForAttendee(liveEventInfo));

                        } else if (foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.livestreaming.LiveStreamingActivity")) {
                            RxBus.INSTANCE.publish(RxEvent.Companion.coHostRequestToHost(liveEventInfo));

                        } else {
                            if (map3 != null) {
                                Notification.Builder mBuilder =
                                        new Notification.Builder(this)
                                                .setSmallIcon(R.mipmap.ic_launcher)
                                                .setContentTitle(maps2.optString("title"))
                                                .setContentText(maps2.optString("body"))
                                                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE) //Important for heads-up notification
                                                .setContentIntent(pendingIntent)
                                                .setGroup("defaultGroup")
                                                .setPriority(Notification.PRIORITY_HIGH)
                                                .setAutoCancel(true)
                                                .setCategory(NotificationCompat.CATEGORY_CALL);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    mBuilder.setChannelId("124562345467446589");
                                }

                                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.notify(0, mBuilder.build());
                            }
                        }
                    } else {
                        if (map3 != null) {
                            Notification.Builder mBuilder =
                                    new Notification.Builder(this)
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setContentTitle(maps2.optString("title"))
                                            .setContentText(maps2.optString("body"))
                                            .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE) //Important for heads-up notification
                                            .setContentIntent(pendingIntent)
                                            .setGroup("defaultGroup")
                                            .setPriority(Notification.PRIORITY_HIGH)
                                            .setAutoCancel(true)
                                            .setCategory(NotificationCompat.CATEGORY_CALL);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                mBuilder.setChannelId("124562345467446589");
                            }

                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(0, mBuilder.build());
                        }
                    }
                } else if (type.equals("chat")) {
                    String msgType = map2.optString("message_type");

                    if (msgType != null) {

                        ChatRoomUser receiver = new ChatRoomUser((map2.optInt("sender_id")), map2.optString("sender_name"), null, map2.optString("sender_profile"), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,map2.optInt("is_verified"));

                        ChatRoomInfo chatRoomInfo = new ChatRoomInfo(null, true, map2.optString("created_at"), map2.optString("message"), map2.optString("message_type"), null, null, null, map2.optString("created_at"), map2.optInt("sender_id"), null, map2.optInt("conversation_id"), map2.optInt("receiver_id"), null, map2.optInt("room_type"), null, null, null, null, null, null, null, null, false, null, null, null, null, null, receiver, null, 0, 1,map2.optInt("is_verified"), false);

                        if (body == null) {
                            body = msgType;
                        }
                        if (foregroundActivity != null) {
                            if (foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity")) {
                                Activity act = ActivityManager.Companion.getInstance().getForegroundActivity();
                                int id = ViewOneToOneChatRoomActivity.Companion.getConversationId((ViewOneToOneChatRoomActivity) act);
                                if (id != chatRoomInfo.getId()) {
                                    generateChatNotification(this, title, body, chatRoomInfo,map2.optInt("receiver_id"));
                                }
                            } else {
                                generateChatNotification(this, title, body, chatRoomInfo,map2.optInt("receiver_id"));
                            }
                        } else {
                            generateChatNotification(this, title, body, chatRoomInfo,map2.optInt("receiver_id"));
                        }

                    }
                } else {
                    String map = bundle.getString("message");
                    JSONObject map3 = null;
                    try {
                        if (map != null) {
                            map3 = new JSONObject(map);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map3 != null) {
                        if (map3.optString("body").equalsIgnoreCase("calling call notification")) {
                            if (map3 != null) {
                                try {
                                    if (map2.getString("type").equalsIgnoreCase("missed")) {
                                        if (map2.getString("call_type").equalsIgnoreCase("audio")) {
                                            generateNotification(this, "Missed call", "You missed a voice call", "", "", 0,0);

                                        } else {
                                            generateNotification(this, "Missed call", "You missed a video call", "", "", 0,0);
                                        }
                                    } else if (map2.getString("type").equalsIgnoreCase("disconnect")) {
                                        if (map2.getString("call_type").equalsIgnoreCase("audio")) {
                                        } else {
                                        }
                                    } else if (map2.getString("type").equalsIgnoreCase("calling")) {
                                        if (map2.getString("call_type").equalsIgnoreCase("video")) {
                                            if (foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.chatRoom.videoCall.OneToOneVideoCallActivity")) {
                                                oneToOneVideoNotification(map2, 1);

                                            } else {
                                                oneToOneVideoNotification(map2, 0);
                                            }
                                        }
                                    } else if (map2.getString("type").equalsIgnoreCase("end_call")) {
                                        RxBus.INSTANCE.publish(RxEvent.Companion.cutCall());
                                    } else if (map2.getString("call_type").equalsIgnoreCase("audio"))
                                        AudioNotification(map2);
                                    else
                                    oneToOneVideoNotification(map2, 0);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else if (map3.optString("body").equalsIgnoreCase("call end notification")) {
                            RxBus.INSTANCE.publish(RxEvent.Companion.cutCall());

                        } else if (map3.optString("body").equalsIgnoreCase("disconnect call notification")) {
                            RxBus.INSTANCE.publish(RxEvent.Companion.cutCall());

                        } else if (map3.optString("body").equalsIgnoreCase("missed call notification")) {
                            try {
                                generateNotification(this, "Missed call", "You missed a video call from " + (map2.getString("from_user_name")), "", "", 0,0);
                            } catch (JSONException e) {
                                generateNotification(this, "Missed call", "You missed a video call", "", "", 0,0);
                            }

                        } else if (map2.optString("type").equals("gift")) {
                            if (foregroundActivity != null) {
                                if (foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.livestreaming.watchlive.WatchLiveStreamingActivity")) {

                                } else if (foregroundActivity.getLocalClassName().equalsIgnoreCase("ui.livestreaming.LiveStreamingActivity")) {

                                } else {
                                    generateNotification(this, map3.optString("title"), map3.optString("body"), map2.optString("type"), map2.optString("type_id"), map2.optInt("from_user"),map2.optInt("to_user"));
                                }
                            }
                        } else {
                            if(map2.optString("type").equals("redeem_request")) {
                                RxBus.INSTANCE.publish(RxEvent.Companion.redeemInfo(map2.optString("status"),map2.optString("amount")));
                            }
                            generateNotification(this, map3.optString("title"), map3.optString("body"), map2.optString("type"), map2.optString("type_id"), map2.optInt("from_user"),map2.optInt("to_user"));
                        }
                    }
                }


            }
        }

    }

    public void AudioNotification(JSONObject map) {
        final int NOTIFY_ID = 1;
        NotificationManager notifManager = null;
        // There are hardcoding only for show it's just strings
        String name = "my_package_channel";
        String id = "12456234546744658"; // The user-visible name of the channel.
        String description = "my_package_first_channel"; // The user-visible description of the channel.

        Intent intent1;
        PendingIntent pendingIntent1;
        NotificationCompat.Builder builder = null;
        Intent intentd = new Intent(this, ActionReceiver.class);
        intentd.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intentd.putExtra("action", "1");
        try {
            intentd.putExtra("friendId", map.getString("from_user"));
            intentd.putExtra("type", map.getString("call_type"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PendingIntent dismissIntent = PendingIntent.getBroadcast(this, 0, intentd, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (notifManager == null) {
            notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableVibration(true);
                mChannel.setLightColor(Color.GREEN);
                mChannel.setShowBadge(true);
                mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                mChannel.setImportance(importance);
                mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                notifManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(this, id);

            intent1 = new Intent(this, CallLauncherActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            try {
                intent1.putExtra("uId", map.getString("uid"));
                intent1.putExtra("name", map.getString("from_user_name"));
                intent1.putExtra("image", map.getString("from_user_image"));
                intent1.putExtra("id", map.getString("from_user"));
                intent1.putExtra("channelName", map.getString("channelName"));
                intent1.putExtra("accessToken", map.getString("token"));
                intent1.putExtra("type", map.getString("call_type"));
                intent1.putExtra("from", "notification");
                intent1.putExtra("call_type", "audio");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pendingIntent1 = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            long[] pattern = {500, 400, 300, 200, 400};
            final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(pattern, -1);
            v.cancel();

            try {
                builder.setContentTitle("Voice Call...")  // required
                        .setSmallIcon(R.drawable.ic_meet_friend_logo_white) // required
                        .setContentText(map.getString("from_user_name") + " is Calling you....")  // required
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pendingIntent1, true)
                        .setTicker(map.getString("from_user_name") + " is Calling you....")
                        .addAction(0, "Accept", pendingIntent1)
                        .addAction(0, "Reject", dismissIntent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            builder = new NotificationCompat.Builder(this);

            intent1 = new Intent(this, CallLauncherActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            try {
                intent1.putExtra("uId", map.getString("uid"));
                intent1.putExtra("name", map.getString("from_user_name"));
                intent1.putExtra("image", map.getString("from_user_image"));
                intent1.putExtra("id", map.getString("from_user"));
                intent1.putExtra("channelName", map.getString("channelName"));
                intent1.putExtra("accessToken", map.getString("token"));
                intent1.putExtra("from", "notification");
                intent1.putExtra("type", map.getString("call_type"));
                intent1.putExtra("call_type", "audio");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pendingIntent1 = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            try {
                builder.setContentTitle("Voice Call..")                           // required
                        .setSmallIcon(R.drawable.ic_meet_friend_logo_white) // required
                        .setContentText(map.getString("from_user_name") + " is Calling you....")  // required
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pendingIntent1, true)
                        .setTicker(map.getString("from_user_name") + " is Calling you....")
                        .addAction(0, "Reject", dismissIntent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Notification notifications = builder.build();
        notifManager.notify(NOTIFY_ID, notifications);

    }

    public void oneToOneVideoNotification(JSONObject map,Integer isFrom) throws JSONException {
        final int NOTIFY_ID = 1;
        NotificationManager notifManager = null;
        // There are hardcoding only for show it's just strings
        String name = "my_package_channel";
        String id = "12456234546744658"; // The user-visible name of the channel.
        String description = "my_package_first_channel"; // The user-visible description of the channel.

        PendingIntent pendingIntent;
        NotificationCompat.Builder builder = null;

        String channelName = map.getString("channelName");
        String chatRoomId = channelName.substring(channelName.lastIndexOf("_") + 1);
        Integer chatRoomId1 = Integer.parseInt(chatRoomId);

        Intent intent = new Intent(this, OneToOneVideoCallActivity.class);
        intent.putExtra("INTENT_CHAT_ROOM_ID", chatRoomId1);
        intent.putExtra("action", "1");
        intent.putExtra("isFrom", isFrom);

        Intent intentd = new Intent(this, ActionReceiver.class);
        intentd.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intentd.putExtra("action", "1");

        try {
            intentd.putExtra("friendId", map.getString("from_user"));
            intentd.putExtra("type", map.getString("call_type"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PendingIntent dismissIntent = PendingIntent.getBroadcast(this, 0, intentd, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (notifManager == null) {
            notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableVibration(true);
                mChannel.setLightColor(Color.GREEN);
                mChannel.setShowBadge(true);
                mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                notifManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(this, id);

            long[] pattern = {500, 400, 300, 200, 400};
            final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(pattern, -1);
            v.cancel();

            try {
                builder.setContentTitle("Video Call...")  // required
                        .setSmallIcon(R.mipmap.ic_launcher) // required
                        .setContentText(map.getString("from_user_name") + " is Calling you....")  // required
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                        .setPriority(PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setTicker(map.getString("from_user_name") + " is Calling you....")
                        .addAction(0, "Accept", pendingIntent)
                        .addAction(0, "Reject", dismissIntent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            builder.build();
        } else {
            builder = new NotificationCompat.Builder(this);
            try {
                builder.setContentTitle("Video Call..")                           // required
                        .setSmallIcon(R.mipmap.ic_launcher) // required
                        .setContentText(map.getString("from_user_name") + " is Calling you....")  // required
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setTicker(map.getString("from_user_name") + " is Calling you....")
                        .addAction(0, "Accept", pendingIntent)
                        .addAction(0, "Reject", dismissIntent);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        Notification notifications = builder.build();
        notifications.flags |= Notification.FLAG_AUTO_CANCEL;
        notifManager.notify(NOTIFY_ID, notifications);
    }

    public void generateNotification(Context context, String title, String body, String type, String typeId, Integer fromUserId, Integer toUserId) {

        NotificationManager mNotificationManager;

        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "124562345467446589")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("124562345467446589", "meeturfriends", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            assert mNotificationManager != null;

            mBuilder.setChannelId("124562345467446589");
            mNotificationManager.createNotificationChannel(notificationChannel);
        } else {
            mBuilder.setContentTitle(title)                           // required
                    .setSmallIcon(R.mipmap.ic_launcher) // required
                    .setContentText(body)  // required
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_HIGH);

        }

        Boolean isLoginUser = true;
        if (toUserId != null && toUserId != 0) {
            if (loggedInUserCache.getLoggedInUser() != null
                    && loggedInUserCache.getLoggedInUser().getLoggedInUser() != null
                    && loggedInUserCache.getLoggedInUser().getLoggedInUser().getUserId() != null
                    && loggedInUserCache.getLoggedInUser().getLoggedInUser().getUserId().equals(toUserId)) {
                isLoginUser = true;
            } else {
                isLoginUser = false;
            }
        }

        if (type.equals("post") || type.equals("shorts")) {

            resultIntent = MainHomeActivity.Companion.launchFromPostNotification(context, Integer.parseInt(typeId),(body.contains("comment") && type.equals("shorts")),isLoginUser,toUserId);
        } else if (type.equals("challenge")) {
            resultIntent = MainHomeActivity.Companion.launchFromChallengeNotification(context,Integer.parseInt(typeId),isLoginUser,toUserId);

        }else if (type.equals("Follow")) {
            if (body != null && body.contains("sent you follow request")) {
                resultIntent = MainHomeActivity.Companion.launchFromFollowNotification(context,isLoginUser,toUserId);
            } else {
                resultIntent = MainHomeActivity.Companion.launchProfileFromFollowNotification(context, fromUserId,isLoginUser,toUserId);
            }
        } else if (type.equals("gift")) {
            resultIntent = MainHomeActivity.Companion.launchGiftNotification(context,isLoginUser,toUserId);
        } else if (type.equals("live_event")) {
            resultIntent = MainHomeActivity.Companion.launchFromLiveCoHostNotification(this, Integer.parseInt(typeId), true,isLoginUser,toUserId);
        } else {
            resultIntent = new Intent(context, MainHomeActivity.class);
        }
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(11, mBuilder.build());
        RxBus.INSTANCE.publish(RxEvent.Companion.updateUnreadCount());

    }

    public void generateChatNotification(Context context, String title, String body, ChatRoomInfo chatRoomInfo,Integer toUserId) {
        NotificationManager mNotificationManager;

        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "124562345467446589")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("124562345467446589", "meeturfriends", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            assert mNotificationManager != null;

            mBuilder.setChannelId("124562345467446589");
            mNotificationManager.createNotificationChannel(notificationChannel);
        } else {
            mBuilder.setContentTitle(title)                           // required
                    .setSmallIcon(R.mipmap.ic_launcher) // required
                    .setContentText(body)  // required
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Boolean isLoginUser = true ;
        if(toUserId != 0) {
            if(loggedInUserCache.getLoggedInUser().getLoggedInUser().getUserId() == toUserId) {
                isLoginUser = true;
            } else {
                isLoginUser = false;
            }
        }

        if (body != null && body.contains("have requested you to join chat")) {
            resultIntent = MainHomeActivity.Companion.launchFromChatRequestNotification(context,isLoginUser,toUserId);

        } else {
            resultIntent = MainHomeActivity.Companion.launchFromMessageNotification(context, chatRoomInfo,isLoginUser,toUserId);

        }

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(11, mBuilder.build());


        if (chatRoomInfo.getRoomType() == 3) {
            RxBus.INSTANCE.publish(RxEvent.Companion.updateUnreadCount());
        }
    }
}
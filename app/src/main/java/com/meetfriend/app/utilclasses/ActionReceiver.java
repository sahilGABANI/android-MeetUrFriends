package com.meetfriend.app.utilclasses;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.meetfriend.app.utils.FileUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        String friendId = intent.getStringExtra("friendId");
        int receiverId =Integer.parseInt(friendId);


        String tokenL = FileUtils.INSTANCE.getLoginUserTokenSharedPreference().getString("loggedInUserToken","");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    StringBuilder sb=null;
                    BufferedReader reader=null;
                    String serverResponse=null;
                    try {
                        Map<String,Object> params = new LinkedHashMap<>();
                        params.put("receiver_id", receiverId);

                        sb = new StringBuilder();
                        for (Map.Entry<String,Object> param : params.entrySet()) {
                            if (sb.length() != 0) sb.append('&');
                            sb.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                            sb.append('=');
                            sb.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                        }
                        byte[] postDataBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

                        String token = "Bearer " + tokenL;
                        URL url = new URL("https://api.meeturfriends.com/api/room/end_call");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        connection.setConnectTimeout(5000);
                        connection.setRequestProperty("Authorization", token);
                        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                        connection.setRequestMethod("POST");
                        connection.getOutputStream().write(postDataBytes);
                        connection.connect();


                        int statusCode = connection.getResponseCode();
                        if (statusCode == 200) {
                            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line + "\n");
                            }
                        }

                        connection.disconnect();
                        if (sb!=null)
                            serverResponse=sb.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(Integer.parseInt(action));
    }

}
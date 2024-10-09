package com.meetfriend.app.utilclasses;

import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateUtils;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.meetfriend.app.responseclasses.ChatStatusPOJO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import contractorssmart.app.utilsclasses.PreferenceHandler;

public class UtilsClass {
    public static String FIREBASE_USER_STATUS = "user_status";
    public static String NODE_STATUS = "status";
    public static String NODE_LAST_SEEN = "last_seen";
    public static String FIREBASE_NODE = "chat";

    public static void updateUserStatus(Context mContext, boolean status) {
        String FIREBASE_USERS = "users";

        String user_id = PreferenceHandler.INSTANCE.readString(mContext, "USER_ID", "");
        String userName = PreferenceHandler.INSTANCE.readString(mContext, "FIRSTNAME", "");
        String lastName = PreferenceHandler.INSTANCE.readString(mContext, "LASTNAME", "");
        String userImage = PreferenceHandler.INSTANCE.readString(mContext, "PROFILE_PHOTO", "");
        FirebaseDatabase.getInstance().getReference().child(FIREBASE_USER_STATUS)
                .child(user_id).child(NODE_STATUS).setValue(status);
        FirebaseDatabase.getInstance().getReference().child(FIREBASE_USER_STATUS)
                .child(user_id).child(NODE_LAST_SEEN).setValue(ServerValue.TIMESTAMP);

        DatabaseReference fireTable = FirebaseDatabase.getInstance().getReference().child(FIREBASE_USER_STATUS)
                .child(user_id).child(FIREBASE_USERS);
        ChatStatusPOJO item;
        item = new ChatStatusPOJO(user_id,
                userName + " " + lastName,
                userImage);

        fireTable.setValue(item);
    }

    public static String getPassedTimeString(Context mContext, String timeStamp) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        long epochInMillis = Long.parseLong(timeStamp);
        Calendar now = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        Calendar timeToCheck = Calendar.getInstance();
        timeToCheck.setTimeInMillis(epochInMillis);
        timeToCheck.setTimeZone(tz);
        if (now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + getTimeFromTimeStamp(mContext, timeStamp);
        } else if (yesterday.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR)) {
            return "Yesterday, " + getTimeFromTimeStamp(mContext, timeStamp);
        } else {
            if (daysBetween(timeToCheck, now) == 1)
                return daysBetween(timeToCheck, now) + " day ago";
            else return daysBetween(timeToCheck, now) + " days ago";
        }
    }

    private static int daysBetween(Calendar day1, Calendar day2) {
        Calendar dayOne = (Calendar) day1.clone(), dayTwo = (Calendar) day2.clone();
        if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
            return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR));
        } else {
            if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
                //swap them
                Calendar temp = dayOne;
                dayOne = dayTwo;
                dayTwo = temp;
            }
            int extraDays = 0;
            int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);
            while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
                dayOne.add(Calendar.YEAR, -1);
                extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
            }
            return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOneOriginalYearDays;
        }
    }

    public static String getTimeFromTimeStamp(Context mContext, String TimeInMilis) {
        return DateUtils.formatDateTime(mContext, Long.parseLong(TimeInMilis), DateUtils.FORMAT_SHOW_TIME);
    }

    public static AlertDialog.Builder showDialog(Context mContext, String message, String title, String positiveText, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, String negativeText, String neutralText, DialogInterface.OnClickListener neutralListener, boolean isCancelable) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setNegativeButton(negativeText, negativeListener);
        alert.setPositiveButton(positiveText, positiveListener);
        alert.setNeutralButton(neutralText, neutralListener);
        alert.setCancelable(isCancelable);
        try {
            alert.show();
        } catch (Exception e) {
        }
        return alert;
    }

    public static String formatDate(String date) {
        SimpleDateFormat sdfmt1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfmt2 = new SimpleDateFormat("MM/dd/yyyy");

        Date dDate1 = null;
        try {
            dDate1 = sdfmt1.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return sdfmt2.format(dDate1);
    }
}

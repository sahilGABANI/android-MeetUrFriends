package com.meetfriend.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UiUtils {
    public static void hideKeyboard(final Context context) {
        if (context == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextWrapper) {
            Context parentContext = ((ContextWrapper) context).getBaseContext();
            if (parentContext instanceof Activity) {
                activity = (Activity) parentContext;
            }
        }

        if (activity == null) {
            return;
        }

        if (activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } else {
        }
    }

    public static void showKeyboard(final Context context) {
        if (context == null) {
            return;
        }
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextWrapper) {
            Context parentContext = ((ContextWrapper) context).getBaseContext();
            if (parentContext instanceof Activity) {
                activity = (Activity) parentContext;
            }
        }

        if (activity == null) {
            return;
        }

        final View currentFocusView = activity.getCurrentFocus();
        if (currentFocusView != null) {
            currentFocusView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    inputMethodManager.showSoftInput(currentFocusView, 0);
                }
            }, 100);
        } else {
        }
    }


    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get the status bar height set in the internal android configuration.
     *
     * @param context context for resources
     * @return status bar height or 0 if it isn't set
     */

    /**
     * Get the display height in pixels
     *
     * @param activityContext context for getting windowManage
     * @return status bar height or 0 if it isn't set
     */
    public static int getDisplayHeightInPx(Activity activityContext) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activityContext.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }


    public static boolean hideKeyboard(@NonNull Window window) {
        View view = window.getCurrentFocus();
        return hideKeyboard(window, view);
    }

    private static boolean hideKeyboard(@NonNull Window window, @Nullable View view) {
        if (view == null) {
            return false;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) window.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            return inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        return false;
    }
}

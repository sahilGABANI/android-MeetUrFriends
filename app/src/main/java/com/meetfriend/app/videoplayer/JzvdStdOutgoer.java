package com.meetfriend.app.videoplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.meetfriend.app.R;
import com.outgoer.videoplayer.VideoDoubleClick;


public class JzvdStdOutgoer extends JzvdStd {
    public boolean isVideMute = false;
    private String videoUrl;
    private VideoDoubleClick videoDoubleClick;

    public String getVideoUrl() {
        String url = videoUrl;
        return url;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }



    public void setVideoDoubleClick(VideoDoubleClick videoDoubleClick) {
        this.videoDoubleClick = videoDoubleClick;
    }

    public JzvdStdOutgoer(Context context) {
        super(context);
    }

    public JzvdStdOutgoer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        bottomContainer.setVisibility(GONE);
        topContainer.setVisibility(GONE);
        bottomProgressBar.setVisibility(VISIBLE);
    }

    @Override
    public int getLayoutId() {
        return R.layout.jz_layout_std_outgoer;
    }

    //changeUiTo 真能能修改ui的方法
    @Override
    public void changeUiToNormal() {
        super.changeUiToNormal();
        bottomContainer.setVisibility(GONE);
        topContainer.setVisibility(GONE);
    }

    @Override
    public void onStatePreparing() {
        super.onStatePreparing();
        posterImageView.setVisibility(VISIBLE);
    }

    @Override
    public void setAllControlsVisiblity(int topCon, int bottomCon, int startBtn, int loadingPro,
                                        int posterImg, int bottomPro, int retryLayout) {
        topContainer.setVisibility(topCon);
        bottomContainer.setVisibility(bottomCon);
        startButton.setVisibility(startBtn);
        loadingProgressBar.setVisibility(GONE);
        bottomProgressBar.setVisibility(VISIBLE);
        mRetryLayout.setVisibility(retryLayout);
    }

    @Override
    public void dissmissControlView() {
        if (state != STATE_NORMAL
                && state != STATE_ERROR
                && state != STATE_AUTO_COMPLETE) {
            post(() -> {
                bottomContainer.setVisibility(View.INVISIBLE);
                topContainer.setVisibility(View.INVISIBLE);
                startButton.setVisibility(View.INVISIBLE);
                if (clarityPopWindow != null) {
                    clarityPopWindow.dismiss();
                }
                if (screen != SCREEN_TINY) {
                    bottomProgressBar.setVisibility(View.GONE);
                }
            });
        }
    }


    @Override
    public void onClickUiToggle() {
        super.onClickUiToggle();
        startButton.performClick();
        bottomContainer.setVisibility(GONE);
        topContainer.setVisibility(GONE);
    }

    public void updateStartImage() {
        if (state == STATE_PLAYING) {
            startButton.setVisibility(VISIBLE);
            startButton.setImageResource(R.drawable.exo_icon_play);
            replayTextView.setVisibility(GONE);
        } else if (state == STATE_ERROR) {
            startButton.setVisibility(INVISIBLE);
            replayTextView.setVisibility(GONE);
        } else if (state == STATE_AUTO_COMPLETE) {
            startButton.setVisibility(VISIBLE);
            startButton.setImageResource(R.drawable.exo_icon_play);
            replayTextView.setVisibility(VISIBLE);
        } else {
            startButton.setImageResource(R.drawable.exo_icon_play);
            replayTextView.setVisibility(GONE);
        }
    }

    @Override
    public void onStatePlaying() {
        super.onStatePlaying();
        posterImageView.setVisibility(INVISIBLE);
        if (isVideMute) {
            mute();
        } else {
            unMute();
        }
    }

    public void mute() {
        if (mediaInterface != null) {
            mediaInterface.setVolume(0, 0);
        }
    }

    public void unMute() {
        if (mediaInterface != null) {
            mediaInterface.setVolume(1, 1);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouch(v, event);
    }

    protected GestureDetector gestureDetector = new GestureDetector(getContext().getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (videoDoubleClick != null) {
                videoDoubleClick.onDoubleClick();
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }
    });
}
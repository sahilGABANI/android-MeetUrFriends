package com.meetfriend.app.ui.mediapicker.custom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class PressedImageView extends AppCompatImageView {

    private float scaleSize;

    public PressedImageView(Context context) {
        super(context);
        this.scaleSize = 0.97f;
    }

    public PressedImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.scaleSize = 0.97f;
    }

    public PressedImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.scaleSize = 0.97f;
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (isPressed()) {
            setScaleX(this.scaleSize);
            setScaleY(this.scaleSize);
        } else {
            setScaleX(1.0f);
            setScaleY(1.0f);
        }
    }
}
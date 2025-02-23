// Generated by view binder compiler. Do not edit!
package com.redevrx.video_trimmer.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.canhub.cropper.CropImageView;
import com.redevrx.video_trimmer.R;
import com.redevrx.video_trimmer.view.TimeLineView;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ViewCropperBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final CropImageView cropFrame;

  @NonNull
  public final SeekBar cropSeekbar;

  @NonNull
  public final SeekBar handlerTop;

  @NonNull
  public final LinearLayout seekerFrame;

  @NonNull
  public final FrameLayout timeLineFrame;

  @NonNull
  public final TimeLineView timeLineView;

  private ViewCropperBinding(@NonNull RelativeLayout rootView, @NonNull CropImageView cropFrame,
      @NonNull SeekBar cropSeekbar, @NonNull SeekBar handlerTop, @NonNull LinearLayout seekerFrame,
      @NonNull FrameLayout timeLineFrame, @NonNull TimeLineView timeLineView) {
    this.rootView = rootView;
    this.cropFrame = cropFrame;
    this.cropSeekbar = cropSeekbar;
    this.handlerTop = handlerTop;
    this.seekerFrame = seekerFrame;
    this.timeLineFrame = timeLineFrame;
    this.timeLineView = timeLineView;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ViewCropperBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ViewCropperBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.view_cropper, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ViewCropperBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.cropFrame;
      CropImageView cropFrame = ViewBindings.findChildViewById(rootView, id);
      if (cropFrame == null) {
        break missingId;
      }

      id = R.id.cropSeekbar;
      SeekBar cropSeekbar = ViewBindings.findChildViewById(rootView, id);
      if (cropSeekbar == null) {
        break missingId;
      }

      id = R.id.handlerTop;
      SeekBar handlerTop = ViewBindings.findChildViewById(rootView, id);
      if (handlerTop == null) {
        break missingId;
      }

      id = R.id.seekerFrame;
      LinearLayout seekerFrame = ViewBindings.findChildViewById(rootView, id);
      if (seekerFrame == null) {
        break missingId;
      }

      id = R.id.timeLineFrame;
      FrameLayout timeLineFrame = ViewBindings.findChildViewById(rootView, id);
      if (timeLineFrame == null) {
        break missingId;
      }

      id = R.id.timeLineView;
      TimeLineView timeLineView = ViewBindings.findChildViewById(rootView, id);
      if (timeLineView == null) {
        break missingId;
      }

      return new ViewCropperBinding((RelativeLayout) rootView, cropFrame, cropSeekbar, handlerTop,
          seekerFrame, timeLineFrame, timeLineView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}

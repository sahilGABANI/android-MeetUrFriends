package net.vrgsoft.videcrop;

import android.Manifest;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.Statistics;
import com.google.android.exoplayer2.util.Util;
import net.vrgsoft.videcrop.cropview.window.CropVideoView;
import net.vrgsoft.videcrop.player.VideoPlayer;
import net.vrgsoft.videcrop.view.ProgressView;
import net.vrgsoft.videcrop.view.VideoSliceSeekBarH;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;


public class VideoCropActivity extends AppCompatActivity implements VideoPlayer.OnProgressUpdateListener, VideoSliceSeekBarH.SeekBarChangeListener {

    private static final String VIDEO_CROP_INPUT_PATH = "VIDEO_CROP_INPUT_PATH";
    private static final String VIDEO_CROP_OUTPUT_PATH = "VIDEO_CROP_OUTPUT_PATH";
    private static final int STORAGE_REQUEST = 100;
    private VideoPlayer mVideoPlayer;
    private StringBuilder formatBuilder;
    private Formatter formatter;
    private AppCompatImageView mIvPlay;
    private AppCompatImageView mIvAspectRatio;
    private AppCompatImageView mIvDone;
    private VideoSliceSeekBarH mTmbProgress;
    private CropVideoView mCropVideoView;
    private TextView mTvProgress;
    private TextView mTvDuration;
    private TextView mTvAspectCustom;
    private TextView mTvAspectSquare;
    private TextView mTvAspectPortrait;
    private TextView mTvAspectLandscape;
    private TextView mTvAspect4by3;
    private TextView mTvAspect16by9;
    private TextView mTvCropProgress;
    private View mAspectMenu;
    private ProgressView mProgressBar;
    private String inputPath;
    private String outputPath;
    private boolean isVideoPlaying = false;
    private boolean isAspectMenuShown = false;
    private String TAG = "VideoCropActivity";

    public static Intent createIntent(Context context, String inputPath, String outputPath) {
        Intent intent = new Intent(context, VideoCropActivity.class);
        intent.putExtra(VIDEO_CROP_INPUT_PATH, inputPath);
        intent.putExtra(VIDEO_CROP_OUTPUT_PATH, outputPath);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        inputPath = getIntent().getStringExtra(VIDEO_CROP_INPUT_PATH);
        outputPath = getIntent().getStringExtra(VIDEO_CROP_OUTPUT_PATH);

        if (TextUtils.isEmpty(inputPath) || TextUtils.isEmpty(outputPath)) {
            Toast.makeText(this, "input and output paths must be valid and not null", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }

        findViews();
        initListeners();

        requestStoragePermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPlayer(inputPath);
                } else {
                    Toast.makeText(this, "You must grant a write storage permission to use this functionality", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isVideoPlaying) {
            mVideoPlayer.play(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoPlayer != null) {
            mVideoPlayer.play(false);
        } else {
            Log.w("VideoCropActivity", "VideoPlayer is null in onStop");
        }
    }

    @Override
    public void onDestroy() {
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
        } else {
            Log.w("VideoCropActivity", "VideoPlayer is null in onStop");
        }
        super.onDestroy();
    }

    @Override
    public void onFirstTimeUpdate(long duration, long currentPosition) {
        mTmbProgress.setSeekBarChangeListener(this);
        mTmbProgress.setMaxValue(duration);
        mTmbProgress.setLeftProgress(0);
        mTmbProgress.setRightProgress(duration);
        mTmbProgress.setProgressMinDiff(0);
    }

    @Override
    public void onProgressUpdate(long currentPosition, long duration, long bufferedPosition) {
        mTmbProgress.videoPlayingProgress(currentPosition);
        if (!mVideoPlayer.isPlaying() || currentPosition >= mTmbProgress.getRightProgress()) {
            if (mVideoPlayer.isPlaying()) {
                playPause();
            }
        }

        mTmbProgress.setSliceBlocked(false);
        mTmbProgress.removeVideoStatusThumb();

//        mTmbProgress.setPosition(currentPosition);
//        mTmbProgress.setBufferedPosition(bufferedPosition);
//        mTmbProgress.setDuration(duration);
    }

    private void findViews() {
        mCropVideoView = findViewById(R.id.cropVideoView);
        mIvPlay = findViewById(R.id.ivPlay);
        mIvAspectRatio = findViewById(R.id.ivAspectRatio);
        mIvDone = findViewById(R.id.ivDone);
        mTvProgress = findViewById(R.id.tvProgress);
        mTvDuration = findViewById(R.id.tvDuration);
        mTmbProgress = findViewById(R.id.tmbProgress);
        mAspectMenu = findViewById(R.id.aspectMenu);
        mTvAspectCustom = findViewById(R.id.tvAspectCustom);
        mTvAspectSquare = findViewById(R.id.tvAspectSquare);
        mTvAspectPortrait = findViewById(R.id.tvAspectPortrait);
        mTvAspectLandscape = findViewById(R.id.tvAspectLandscape);
        mTvAspect4by3 = findViewById(R.id.tvAspect4by3);
        mTvAspect16by9 = findViewById(R.id.tvAspect16by9);
        mProgressBar = findViewById(R.id.pbCropProgress);
        mTvCropProgress = findViewById(R.id.tvCropProgress);
    }

    private void initListeners() {
        mIvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPause();
            }
        });
        mIvAspectRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMenuVisibility();
            }
        });
        mIvAspectRatio.setColorFilter(ContextCompat.getColor(this, R.color.black));

        mTvAspectCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropVideoView.setFixedAspectRatio(false);
                handleMenuVisibility();
            }
        });
        mTvAspectSquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropVideoView.setFixedAspectRatio(true);
                mCropVideoView.setAspectRatio(10, 10);
                handleMenuVisibility();
            }
        });
        mTvAspectPortrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropVideoView.setFixedAspectRatio(true);
                mCropVideoView.setAspectRatio(8, 16);
                handleMenuVisibility();
            }
        });
        mTvAspectLandscape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropVideoView.setFixedAspectRatio(true);
                mCropVideoView.setAspectRatio(16, 8);
                handleMenuVisibility();
            }
        });
        mTvAspect4by3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropVideoView.setFixedAspectRatio(true);
                mCropVideoView.setAspectRatio(4, 3);
                handleMenuVisibility();
            }
        });
        mTvAspect16by9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropVideoView.setFixedAspectRatio(true);
                mCropVideoView.setAspectRatio(16, 9);
                handleMenuVisibility();
            }
        });
        mIvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCropStart();
            }
        });
    }

    private void playPause() {
        isVideoPlaying = !mVideoPlayer.isPlaying();
        if (mVideoPlayer.isPlaying()) {
            mVideoPlayer.play(!mVideoPlayer.isPlaying());
            mTmbProgress.setSliceBlocked(false);
            mTmbProgress.removeVideoStatusThumb();
            mIvPlay.setImageResource(R.drawable.ic_play);
            return;
        }
        mVideoPlayer.seekTo(mTmbProgress.getLeftProgress());
        mVideoPlayer.play(!mVideoPlayer.isPlaying());
        mTmbProgress.videoPlayingProgress(mTmbProgress.getLeftProgress());
        mIvPlay.setImageResource(R.drawable.ic_pause);
    }

    private void initPlayer(String uri) {
        if (!new File(uri).exists()) {
            Toast.makeText(this, "File doesn't exists", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mVideoPlayer = new VideoPlayer(this);
        mCropVideoView.setPlayer(mVideoPlayer.getPlayer());
        mVideoPlayer.initMediaSource(this, uri);
        mVideoPlayer.setUpdateListener(this);

        fetchVideoInfo(uri);
    }

    private void fetchVideoInfo(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri); // Pass the URI directly
            int videoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int videoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int rotationDegrees = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

            mCropVideoView.initBounds(videoWidth, videoHeight, rotationDegrees);
            Log.d("testing", "Done...........");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("testing", "Exception = " + e);
            // Handle exception (e.g., log it, show an error message)
        } finally {
            try {
                retriever.release(); // Release the retriever to free up resources
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleMenuVisibility() {
        isAspectMenuShown = !isAspectMenuShown;
        TimeInterpolator interpolator;
        if (isAspectMenuShown) {
            interpolator = new DecelerateInterpolator();
        } else {
            interpolator = new AccelerateInterpolator();
        }
        mAspectMenu.animate()
                .translationY(isAspectMenuShown ? 0 : Resources.getSystem().getDisplayMetrics().density * 400)
                .alpha(isAspectMenuShown ? 1 : 0)
                .setInterpolator(interpolator)
                .start();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                        STORAGE_REQUEST);
            } else {
                initPlayer(inputPath);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_REQUEST);
            } else {
                initPlayer(inputPath);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void handleCropStart() {
        Rect cropRect = mCropVideoView.getCropRect();
        String crop = String.format("crop=%d:%d:%d:%d:exact=0", cropRect.right, cropRect.bottom, cropRect.left, cropRect.top);
        String[] command = {
                "-i", inputPath,
                "-filter:v",  crop,
                "-c:a",
                "copy",
                outputPath
        };

        Config.enableStatisticsCallback(new com.arthenica.mobileffmpeg.StatisticsCallback() {
            @Override
            public void apply(Statistics statistics) {
                final long time = statistics.getTime();
                final long duration = statistics.getTime();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int progress = (int) ((time / (float) duration) * 100);
                        mProgressBar.setProgress((int) progress);
                        mTvCropProgress.setText((int) progress + "%");
                    }
                });
            }
        });
        com.arthenica.mobileffmpeg.FFmpeg.executeAsync(command, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                if (returnCode == Config.RETURN_CODE_SUCCESS) {
                    Log.i(TAG, "Video cropped successfully.");
                    Toast.makeText(VideoCropActivity.this, "Video cropped successfully.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    mIvDone.setEnabled(true);
                    mIvPlay.setEnabled(true);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mProgressBar.setProgress(0);
                    mTvCropProgress.setVisibility(View.INVISIBLE);
                    mTvCropProgress.setText("0%");
                    Toast.makeText(VideoCropActivity.this, "FINISHED", Toast.LENGTH_SHORT).show();

                    Toast.makeText(VideoCropActivity.this, "Error cropping video: " + returnCode, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error cropping video: " + returnCode);
                }
            }
        });
        mIvDone.setEnabled(false);
        mIvPlay.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress(0);
        mTvCropProgress.setVisibility(View.VISIBLE);
        mTvCropProgress.setText("0%");
    }


    @Override
    public void seekBarValueChanged(long leftThumb, long rightThumb) {
        if (mTmbProgress.getSelectedThumb() == 1) {
            mVideoPlayer.seekTo(leftThumb);
        }

        mTvDuration.setText(Util.getStringForTime(formatBuilder, formatter, rightThumb));
        mTvProgress.setText(Util.getStringForTime(formatBuilder, formatter, leftThumb));
    }
}

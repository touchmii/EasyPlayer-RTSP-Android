package org.easydarwin.easyplayer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.easydarwin.easyplayer.data.VideoSource;
import org.easydarwin.easyplayer.databinding.ActivityMainBinding;
import org.easydarwin.easyplayer.fragments.ImageFragment;
import org.easydarwin.easyplayer.fragments.PlayFragment;
import org.easydarwin.video.Client;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class PlayActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0x111;
    private GestureDetectorCompat mDetector;
    private SoundPool mSoundPool;
    private int mTalkPictureSound;
    private int mActionStartSound;
    private int mActionStopSound;
    private PlayFragment mRenderFragment;
    private float mAudioVolumn;
    private float mMaxVolume;
    private ActivityMainBinding mBinding;
    private long mLastReceivedLength;

    private final Handler mHandler = new Handler();
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long length = mRenderFragment.getReceivedStreamLength();
            if (length == 0) {
                mLastReceivedLength = 0;
            }
            if (length < mLastReceivedLength) {
                mLastReceivedLength = 0;
            }
            mBinding.liveVideoBar.streamBps.setText((length - mLastReceivedLength) / 1024 + "Kbps");
            mLastReceivedLength = length;

            mHandler.postDelayed(this, 1000);
        }
    };
    private Runnable mResetRecordStateRunnable = new Runnable() {
        @Override
        public void run() {
            ImageView mPlayAudio = mBinding.liveVideoBar.liveVideoBarRecord;
            mPlayAudio.setImageState(new int[]{}, true);
            mPlayAudio.removeCallbacks(mResetRecordStateRunnable);
        }
    };

    public void onEnableOrDisablePlayAudio(View view) {
        boolean enable = mRenderFragment.toggleAudioEnable();
        ImageView mPlayAudio = (ImageView) view;
        mPlayAudio.setImageState(enable ? new int[]{android.R.attr.state_pressed} : new int[]{}, true);
    }

    public void onTakePicture(View view) {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            File file = new File(TheApp.sPicturePath);
            file.mkdirs();
            file = new File(file, new SimpleDateFormat("yy_MM_dd HH_mm_ss").format(new Date()) + ".jpg");
            mRenderFragment.takePicture(file.getPath());
            if (mSoundPool != null) {
                mSoundPool.play(mTalkPictureSound, mAudioVolumn, mAudioVolumn, 1, 0, 1.0f);
            }
        } else {
            requestWriteStorage(true);
        }
    }

    private void requestWriteStorage(final boolean toTakePicture) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            // Show an expanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.

            new AlertDialog.Builder(this).setMessage(toTakePicture ? "EasyPlayer需要使用写文件权限来抓拍" : "EasyPlayer需要使用写文件权限来录像").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.requestPermissions(PlayActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE + (toTakePicture ? 0 : 1));
                }
            }).show();
        } else {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE + (toTakePicture ? 0 : 1));

            // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE + 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
                        onTakePicture(mBinding.liveVideoBar.liveVideoBarTakePicture);
                    } else {
                        onRecordOrStop(mBinding.liveVideoBar.liveVideoBarRecord);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }
        }
    }

    public void onRecordOrStop(View view) {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (mRenderFragment != null) {
                boolean recording = mRenderFragment.onRecordOrStop();
                ImageView mPlayAudio = (ImageView) view;
                mPlayAudio.setImageState(recording ? new int[]{android.R.attr.state_checked} : new int[]{}, true);

                if (recording) mPlayAudio.postDelayed(mResetRecordStateRunnable, 200);
            }
        } else {
            requestWriteStorage(false);
        }
    }

    protected void initSoundPool() {
        if (true)
            return;
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioVolumn = (float) mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mMaxVolume = (float) mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mSoundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attributes).build();
        } else {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_NOTIFICATION, 0);
        }
        mTalkPictureSound = mSoundPool.load("/system/media/audio/ui/camera_click.ogg", 1);
        mActionStartSound = mSoundPool.load(this, R.raw.action_start, 1);
        mActionStopSound = mSoundPool.load(this, R.raw.action_stop, 1);
    }

    protected void releaseSoundPool() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    public void onTakePictureThumbClicked(View view) {
        String path = (String) view.getTag();
//        ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, "gallery_image_view");
//        Intent intent = new Intent(this, ImageActivity.class);
//        intent.putExtra("extra-uri", Uri.fromFile(new File(path)));
////        ActivityCompat.startActivity(this, intent, compat.toBundle());
//        startActivity(intent);
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, ImageFragment.newInstance(Uri.fromFile(new File(path)))).addToBackStack(null).commit();
    }

    /**
     * 切换屏幕方向
     */
    public void onToggleOrientation() {
        setRequestedOrientation(isLandscape() ?
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate a value suitable for use in View.setId(int)
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("play_url");
        int transportMode = getIntent().getIntExtra(VideoSource.TRANSPORT_MODE, 0);
        int sendOption = getIntent().getIntExtra(VideoSource.SEND_OPTION, 0);
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {

            ResultReceiver rr = getIntent().getParcelableExtra("rr");
            if (rr == null) {
                rr = new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        if (resultCode == PlayFragment.RESULT_REND_STARTED) {
                            onPlayStart();
                        } else if (resultCode == PlayFragment.RESULT_REND_STOPED) {
                            onPlayStoped();
                        } else if (resultCode == PlayFragment.RESULT_REND_VIDEO_DISPLAYED) {
                            onVideoDisplayed();
                        }
                    }
                };
            }
            PlayFragment fragment = PlayFragment.newInstance(url, transportMode,sendOption, rr);
            getSupportFragmentManager().beginTransaction().add(R.id.render_holder, fragment).commit();
            mRenderFragment = fragment;
        } else {
            mRenderFragment = (PlayFragment) getSupportFragmentManager().findFragmentById(R.id.render_holder);
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initSoundPool();

        mBinding.liveVideoBar.liveVideoBarEnableAudio.setEnabled(false);
        mBinding.liveVideoBar.liveVideoBarTakePicture.setEnabled(false);
        mBinding.liveVideoBar.liveVideoBarRecord.setEnabled(false);

        LinearLayout container = mBinding.playerContainer;
        if (isLandscape()) {

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(false);
            // 横屏情况下,播放窗口横着排开
            container.setOrientation(LinearLayout.HORIZONTAL);
            mBinding.renderHolder.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            mRenderFragment.enterFullscreen();
        } else { // 竖屏,取消全屏状态
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(true);
            // 竖屏情况下,播放窗口竖着排开
            container.setOrientation(LinearLayout.VERTICAL);
            mBinding.renderHolder.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.render_wnd_height);
            mRenderFragment.quiteFullscreen();
        }
        mBinding.msgTxt.setMovementMethod(new ScrollingMovementMethod());
    }

    private void onVideoDisplayed() {
        mBinding.liveVideoBar.liveVideoBarTakePicture.setEnabled(true);
        mBinding.liveVideoBar.liveVideoBarRecord.setEnabled(true);
    }

    private void onPlayStart() {
        boolean enable = mRenderFragment.isAudioEnable();
        mBinding.liveVideoBar.liveVideoBarEnableAudio.setImageState(enable ? new int[]{android.R.attr.state_pressed} : new int[]{}, true);
        mBinding.liveVideoBar.liveVideoBarEnableAudio.setEnabled(true);
        mHandler.postDelayed(mTimerRunnable, 1000);

        mBinding.liveVideoBar.liveVideoBarTakePicture.setEnabled(false);
        mBinding.liveVideoBar.liveVideoBarRecord.setEnabled(false);
    }

    private void onPlayStoped() {
        mBinding.liveVideoBar.liveVideoBarEnableAudio.setEnabled(false);
        mHandler.removeCallbacks(mTimerRunnable);
    }

    @Override
    protected void onDestroy() {
        releaseSoundPool();
        super.onDestroy();
    }

    private boolean isLandscape() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LinearLayout container = mBinding.playerContainer;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(false);
            // 横屏情况下,播放窗口横着排开
            container.setOrientation(LinearLayout.HORIZONTAL);
            mBinding.renderHolder.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            mRenderFragment.enterFullscreen();
        } else {
            // 竖屏,取消全屏状态
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(true);
            // 竖屏情况下,播放窗口竖着排开
            container.setOrientation(LinearLayout.VERTICAL);
            mBinding.renderHolder.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.render_wnd_height);
            mRenderFragment.quiteFullscreen();
        }
    }

    public boolean multiWindows() {
        LinearLayout container = (LinearLayout) findViewById(R.id.player_container);
        return container.getChildCount() > 1;
    }

    public void setNavVisibility(boolean visible) {
        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (!visible) {
                // } else {
                // newVis &= ~(View.SYSTEM_UI_FLAG_LOW_PROFILE |
                // View.SYSTEM_UI_FLAG_FULLSCREEN |
                // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                newVis |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
            }
            // If we are now visible, schedule a timer for us to go invisible.
            // Set the new desired visibility.
            getWindow().getDecorView().setSystemUiVisibility(newVis);
        }
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_url) {

        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onOpenFileDirectory(View view) {
        Intent i = new Intent(this, MediaFilesActivity.class);
        startActivity(i);
    }

    public void onEvent(PlayFragment playFragment,int err, String msg) {
        mBinding.msgTxt.append(String.format("[%s]\t%s\n",new SimpleDateFormat("HH:mm:ss").format(new Date()),msg));
    }

    public void onRecordState(int status) {
        ImageView mPlayAudio =
                mBinding.liveVideoBar.liveVideoBarRecord;
        mPlayAudio.setImageState(status == 1 ? new int[]{android.R.attr.state_checked} : new int[]{}, true);
        mPlayAudio.removeCallbacks(mResetRecordStateRunnable);
    }

    public void onFullscreen(View view) {
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setNavVisibility(false);
        // 横屏情况下,播放窗口横着排开
        mBinding.renderHolder.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        mRenderFragment.enterFullscreen();
    }

    @Override
    public void onBackPressed() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if ((attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(true);
            mBinding.renderHolder.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.render_wnd_height);
            mBinding.renderHolder.requestLayout();

            mRenderFragment.quiteFullscreen();

            return;
        }
        super.onBackPressed();
    }
}

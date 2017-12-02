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

import org.easydarwin.easyplayer.fragments.ImageFragment;
import org.easydarwin.easyplayer.fragments.PlayFragment;
import org.easydarwin.video.RTSPClient;
import org.esaydarwin.rtsp.player.R;
import org.esaydarwin.rtsp.player.databinding.ActivityMainBinding;
import org.esaydarwin.rtsp.player.databinding.ActivityTwoWndPlayBinding;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class TwoWndPlayActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0x111;
    private GestureDetectorCompat mDetector;
    private SoundPool mSoundPool;
    private int mTalkPictureSound;
    private int mActionStartSound;
    private int mActionStopSound;
    private PlayFragment mRenderFragment;
    private float mAudioVolumn;
    private float mMaxVolume;
    private ActivityTwoWndPlayBinding mBinding;
    private long mLastReceivedLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("play_url");
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {
            boolean useUDP = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_udp_mode), false);
            PlayFragment fragment = PlayFragment.newInstance(url, useUDP ? RTSPClient.TRANSTYPE_UDP : RTSPClient.TRANSTYPE_TCP, null);
            getSupportFragmentManager().beginTransaction().add(R.id.render_holder, fragment,"first").commit();
            mRenderFragment = fragment;

            fragment = PlayFragment.newInstance("rtsp://cloud.easydarwin.org:554/270051.sdp", useUDP ? RTSPClient.TRANSTYPE_UDP : RTSPClient.TRANSTYPE_TCP, null);
            getSupportFragmentManager().beginTransaction().add(R.id.render_holder, fragment,"second").commit();
        } else {
            mRenderFragment = (PlayFragment) getSupportFragmentManager().findFragmentByTag("first");
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_two_wnd_play);

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


    public void onSwitchPlayer(View view) {
        PlayFragment f = (PlayFragment) getSupportFragmentManager().findFragmentByTag("first");
        PlayFragment s = (PlayFragment) getSupportFragmentManager().findFragmentByTag("second");

        if (!s.isHidden()){
            getSupportFragmentManager().beginTransaction().show(f).commit();
            getSupportFragmentManager().beginTransaction().hide(s).commit();
        }else{
            getSupportFragmentManager().beginTransaction().show(s).commit();
            getSupportFragmentManager().beginTransaction().hide(f).commit();
        }
    }
}

package org.easydarwin.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

/**
 * Created by John on 2017/1/10.
 */

public class EasyMuxer {

    public static final boolean VERBOSE = true;
    private static final String TAG = EasyMuxer.class.getSimpleName();
    private final String mFilePath;
    private MediaMuxer mMuxer;
    private final long durationMillis;
    private int index = 0;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private long mBeginMillis = -1l;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;

    public EasyMuxer(String path, long durationMillis) {
        if (TextUtils.isEmpty(path)){
            throw new InvalidParameterException("path should not be empty!");
        }
        if (path.toLowerCase().endsWith(".mp4")){
            path = path.substring(0, path.toLowerCase().lastIndexOf(".mp4"));
        }
        mFilePath = path;
        this.durationMillis = durationMillis;
        Object mux = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mux = new MediaMuxer(path + "-" + index++ + ".mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mMuxer = (MediaMuxer) mux;
        }
    }

    public synchronized void addTrack(MediaFormat format, boolean isVideo) {
        // now that we have the Magic Goodies, start the muxer
        if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1)
            throw new RuntimeException("already add all tracks");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (isVideo) {
                int track = mMuxer.addTrack(format);
                if (VERBOSE)
                    Log.i(TAG, String.format("addTrack %s result %d", isVideo ? "video" : "audio", track));
                mVideoFormat = format;
                mVideoTrackIndex = track;
                if (mAudioTrackIndex != -1) {
                    if (VERBOSE)
                        Log.i(TAG, "both audio and video added,and muxer is started");
                    mMuxer.start();
                }
            } else {
                if (format != null) {
                    int track = mMuxer.addTrack(format);
                    if (VERBOSE)
                        Log.i(TAG, String.format("addTrack %s result %d", isVideo ? "video" : "audio", track));
                    mAudioFormat = format;
                    mAudioTrackIndex = track;
                }
                if (mVideoTrackIndex != -1) {
                    mMuxer.start();
                }
            }
        }
    }

    public synchronized void pumpStream(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        if (mVideoTrackIndex == -1) {
            Log.i(TAG, String.format("pumpStream [%s] but muxer is not start.ignore..", isVideo ? "video" : "audio"));
            return;
        }
        if (!isVideo){
            if (mAudioTrackIndex == -1) return;
        }
        else{
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){
                Log.w(TAG, String.format("pumpStream [%s] and key frame gotten ... and set mBeginMillis", isVideo ? "video" : "audio"));
                mBeginMillis = SystemClock.elapsedRealtime();
            }
        }
        if (mBeginMillis < 0) {
            Log.w(TAG, String.format("pumpStream [%s] but key frame not gotten yet.ignore..", isVideo ? "video" : "audio"));
            return;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
        } else if (bufferInfo.size != 0) {
            if (isVideo && mVideoTrackIndex == -1) {
                throw new InvalidParameterException("muxer hasn't started");
            }

            // adjust the ByteBuffer values to match BufferInfo (not needed?)
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
            if (VERBOSE)
                Log.d(TAG, String.format("sent %s [" + bufferInfo.size + "] with timestamp:[%d] to muxer", isVideo ? "video" : "audio", bufferInfo.presentationTimeUs / 1000));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMuxer.writeSampleData(isVideo ? mVideoTrackIndex : mAudioTrackIndex, outputBuffer, bufferInfo);
            }

        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM received");
        }

        if (SystemClock.elapsedRealtime() - mBeginMillis >= durationMillis && isVideo && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (VERBOSE)
                    Log.i(TAG, String.format("record file reach expiration.create new file:" + index));
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
                mVideoTrackIndex = mAudioTrackIndex = -1;
                try {
                    mMuxer = new MediaMuxer(mFilePath + "-" + index++ + ".mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    addTrack(mVideoFormat, true);
                    addTrack(mAudioFormat, false);
                    pumpStream(outputBuffer, bufferInfo, isVideo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (mMuxer != null) {
                if (mAudioTrackIndex != -1 || mVideoTrackIndex != -1) {
                    if (VERBOSE)
                        Log.i(TAG, String.format("muxer is started. now it will be stoped."));
                    try {
                        mMuxer.stop();
                        mMuxer.release();
                    } catch (IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}

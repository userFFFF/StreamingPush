package com.user.streamingpush;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by user0 on 2017/11/14.
 */

public class AudioCodec {
    private static final String TAG = "AudioCodec";
    private int mSamplerate;
    private int mChannelConfig;
    private int mSampleType;

    MediaCodec mMediaCodec;
    private static final int timeoutUs = 50000; // 50 ms
    private static final String mime = "audio/mp4a-latm";

    // Dump Output
    private String mDumpOutputPath;
    private static final boolean mDumpOutput = true;
    private static final String mDumpBasePath = Environment.getExternalStorageDirectory() + "/Movies/AAC_";

    public void initAudioEncoder(int samplerate, int channelconfig, int sampletype) {
        this.mSamplerate = samplerate;
        this.mChannelConfig = channelconfig;
        this.mSampleType = sampletype;

        if (mDumpOutput) {
            SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = sTimeFormat.format(new Date());
            mDumpOutputPath = mDumpBasePath + date + ".aac";
        }
        try {
            mMediaCodec = MediaCodec.createEncoderByType(mime);

            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, this.mSamplerate, this.mChannelConfig);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1000 * 122);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, this.mSampleType);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPcmFrameEncoding(byte[] input) {
        if (input == null) {
            Log.e(TAG, "The input buffer is NULL");
            return;
        }
        Log.d(TAG, "onPcmFrameEncoding...");
        Log.d(TAG, "input.length = " + input.length);
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();

        try {
            int bufferIndex = mMediaCodec.dequeueInputBuffer(timeoutUs);
            if (bufferIndex >= 0) {
                inputBuffers[bufferIndex].clear();
                mMediaCodec.queueInputBuffer(bufferIndex, 0,
                        inputBuffers[bufferIndex].position(),
                        System.nanoTime() / 1000, 0);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {

                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] output = new byte[bufferInfo.size];
                    outputBuffer.get(output);

                    Log.d(TAG, "output.length = " + output.length);

                    if (mDumpOutput) {
                        Utils.dumpOutputBuffer(output, 0, output.length, mDumpOutputPath, true);
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            } else {
                Log.e(TAG, "No buffer available");
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stack = sw.toString();
            Log.e("save_log", stack);
            e.printStackTrace();
        } finally {
            return;
        }
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

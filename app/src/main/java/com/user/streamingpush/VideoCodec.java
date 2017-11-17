package com.user.streamingpush;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
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
 * Created by user0 on 2017/11/9.
 */

public class VideoCodec {
    private static final String TAG = "VideoCodec";
    // parameters for the encoder
    private static final int GOP = 1; // second
    private static final int timeoutUs = 50000; // 50 ms
    private static final String mime = "video/avc"; // H.264 Advanced Video

    private int mWidth;
    private int mHeight;
    private int mFramerate;
    private int mBitrate;
    private int mDegree;
    private int mColorFormat;
    private int mYPadding = 0; // some video codec may be used
    private boolean mPlanar = false;
    private boolean mPanesReversed = false; // some video codec may be used
    private MediaCodec mMediaCodec = null;

    // Dump Output
    private String mDumpOutputPath;
    private static final boolean mDumpOutput = true;
    private static final String mDumpBasePath = Environment.getExternalStorageDirectory() + "/Movies/h264_";

    //RTMP Pusher
    RtmpLive mRtmpLive;

    public void setPlanar(boolean planar) {
        mPlanar = planar;
    }

    public boolean getPlanar() {
        return mPlanar;
    }

    @SuppressLint("NewApi")
    public void initVideoEncoder(int width, int height, int degree, int framerate, int bitrate, RtmpLive rtmpLive) {
        Log.d(TAG, "initVideoEncoder");
        this.mWidth = width;
        this.mHeight = height;
        this.mFramerate = framerate;
        this.mBitrate = bitrate;
        this.mDegree = degree;
        this.mRtmpLive = rtmpLive;

        if (mDumpOutput) {
            SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = sTimeFormat.format(new Date());
            mDumpOutputPath = mDumpBasePath + width + "x" + height + "_" + date + ".h264";
        }

        MediaCodecInfo codecInfo = selectCodec(mime);
        if (codecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + mime);
            return;
        }
        mColorFormat = selectColorFormat(codecInfo, mime);
        setEncoderColorFormat(mColorFormat);

        Log.d(TAG, "Video Encoder Parameter: [width = " + width + ", height = " + height + ", degree = " + degree
                + ", framerate = " + framerate + ", bitrate = " + bitrate + "]");

        Log.d(TAG, "Video Encoder Parameter: [colorFormat = " + mColorFormat
                + ", mPlanar = " + mPlanar + ", codec = " + codecInfo.getName() + ", mime_type = " + mime + "]");


        try {
            mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
            MediaFormat mediaFormat;
            if (degree == 0) {
                mediaFormat = MediaFormat.createVideoFormat(mime, height, width);
            } else {
                mediaFormat = MediaFormat.createVideoFormat(mime, width, height);
            }
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, GOP);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] onPreviewFrameEncoding(byte[] input) {
        if (input == null) {
            Log.e(TAG, "The input buffer is NULL");
            return null;
        }
        Log.d(TAG, "onPreviewFrameEncoding...");
        Log.d(TAG, "input.length = " + input.length);
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        byte[] covertBuffer; //= new byte[input.length]; // DO NOT NEED NEW!!!

        if (mDegree == 0) {
            covertBuffer = Utils.rotateNV21Degree90(input, mWidth, mHeight);
        } else {
            covertBuffer = input;
        }

        try {
            int bufferIndex = mMediaCodec.dequeueInputBuffer(timeoutUs);
            if (bufferIndex >= 0) {
                inputBuffers[bufferIndex].clear();
                Utils.convert(covertBuffer, inputBuffers[bufferIndex], mWidth, mHeight, mYPadding, getPlanar(), mPanesReversed);
                mMediaCodec.queueInputBuffer(bufferIndex, 0,
                        inputBuffers[bufferIndex].position(),
                        System.nanoTime() / 1000, 0);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
//                    switch (outputBufferIndex) {
//                        case MediaCodec.INFO_TRY_AGAIN_LATER:
//                            Log.d(TAG, "no output from encoder available");
//                            break;
//                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                            Log.d(TAG, "encoder output buffers changed");
//                            break;
//                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: //Get SPS PPS BUFFER HERE!!!!
//                            MediaFormat mediaFormat = mMediaCodec.getOutputFormat();
//                            ByteBuffer sps = mediaFormat.getByteBuffer("csd-0");
//                            byte[] spsBuffer = new byte[sps.capacity()];
//                            sps.get(spsBuffer);
//
//                            ByteBuffer pps = mediaFormat.getByteBuffer("csd-1");
//                            byte[] ppsBuffer = new byte[pps.capacity()];
//                            sps.get(ppsBuffer);
//                            Log.d(TAG, "encode output SPS & PPS");
//                        default:
//                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//                            outputBuffer.get(output);
//                            break;
//                    }
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] output = new byte[bufferInfo.size];
                    outputBuffer.get(output);
                    long timestamp = System.currentTimeMillis();
                    Log.d(TAG, "output.length = " + output.length);
                    mRtmpLive.StreamPusher(output, output.length, timestamp, Config.MEDIA_TYPE_VIDEO);
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
            return covertBuffer;
        }
    }

    @SuppressLint("NewApi")
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

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG, "couldn't find a good color format for "
                + codecInfo.getName() + " / " + mimeType);

        return 0; // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    public void setEncoderColorFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                setPlanar(false);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                setPlanar(true);
                break;
            default:
                Log.e(TAG, "UnRecognized Color Format");
                break;
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}
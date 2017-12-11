package com.user.streamingpush;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.os.Process;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by user0 on 2017/11/20.
 */

public class AudioStream {
    private static final String TAG = "AudioStream";
    private int mSamplingRate = 44100;
    private int bitRate = 122000;
    private int BUFFER_SIZE = 1920;
    private int mSamplingRateIndex = 0;
    private static final int ADTS_HEADER_SIZE = 7;
    private String MIME = "audio/mp4a-latm";

    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private RtmpLive mRtmpLive;

    private Thread mThread = null;
    private Thread mWriter;

    protected ByteBuffer[] mBuffers = null;
    protected MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    // Dump Output
    private String mDumpOutputPath;
    private static final String mDumpBasePath = Environment.getExternalStorageDirectory() + "/Movies/AAC_";

    public static final int[] AUDIO_SAMPLING_RATES = {
            96000, // 0
            88200, // 1
            64000, // 2
            48000, // 3
            44100, // 4
            32000, // 5
            24000, // 6
            22050, // 7
            16000, // 8
            12000, // 9
            11025, // 10
            8000, // 11
            7350, // 12
            -1, // 13
            -1, // 14
            -1, // 15
    };
    //private MediaFormat newFormat;

    public AudioStream(RtmpLive rtmplive) {
        this.mRtmpLive = rtmplive;
        int i = 0;
        for (; i < AUDIO_SAMPLING_RATES.length; i++) {
            if (AUDIO_SAMPLING_RATES[i] == mSamplingRate) {
                mSamplingRateIndex = i;
                break;
            }
        }
    }

    public void startRecord() {
        if (Config.DumpOutput) {
            SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = sTimeFormat.format(new Date());
            mDumpOutputPath = mDumpBasePath + date + ".aac";
        }
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                int len, bufferIndex;
                try {
                    int bufferSize = AudioRecord.getMinBufferSize(mSamplingRate,
                            AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                    mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            mSamplingRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                    mMediaCodec = MediaCodec.createEncoderByType(MIME);
                    MediaFormat mediaFormat = new MediaFormat();
                    mediaFormat.setString(MediaFormat.KEY_MIME, MIME);
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                    mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
                    mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSamplingRate);
                    mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                            MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                    mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
                    mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mMediaCodec.start();

                    mWriter = new WriterThread();
                    mWriter.start();
                    mAudioRecord.startRecording();
                    final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

                    while (mThread != null) {
                        bufferIndex = mMediaCodec.dequeueInputBuffer(1000);
                        if (bufferIndex >= 0) {
                            inputBuffers[bufferIndex].clear();
                            len = mAudioRecord.read(inputBuffers[bufferIndex], BUFFER_SIZE);
                            if (len == AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, 0,
                                        System.nanoTime() / 1000, 0);
                            } else {
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, len,
                                        System.nanoTime() / 1000, 0);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Audio Record___Error!!!!!");
                    e.printStackTrace();
                } finally {
                    Thread t = mWriter;
                    mWriter = null;
                    while (t != null && t.isAlive()) {
                        try {
                            t.interrupt();
                            t.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    ;
                    if (mAudioRecord != null) {
                        mAudioRecord.stop();
                        mAudioRecord.release();
                        mAudioRecord = null;
                    }
                    if (mMediaCodec != null) {
                        mMediaCodec.stop();
                        mMediaCodec.release();
                        mMediaCodec = null;
                    }
                }
            }
        }, "AAC Record____");
        mThread.start();
    }

    private class WriterThread extends Thread {
        @Override
        public void run() {
            int index;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            } else {
                mBuffers = mMediaCodec.getOutputBuffers();
            }
            ByteBuffer mBuffer = ByteBuffer.allocate(10240);
            do {
                index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 10000);
                if (index >= 0) {
                    if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        continue;
                    }
                    mBuffer.clear();
                    ByteBuffer outputBuffer;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = mMediaCodec.getOutputBuffer(index);
                    } else {
                        outputBuffer = mBuffers[index];
                    }

                    outputBuffer.get(mBuffer.array(), ADTS_HEADER_SIZE, mBufferInfo.size);
                    outputBuffer.clear();
                    mBuffer.position(ADTS_HEADER_SIZE + mBufferInfo.size);
                    adts_write_frame_header(mBuffer.array(), mBufferInfo.size + ADTS_HEADER_SIZE, 0, MediaCodecInfo.CodecProfileLevel.AACObjectLC, 2);
                    mBuffer.flip();
                    long timestamp = System.currentTimeMillis();
                    Log.d(TAG, "mBufferInfo.size: " + mBufferInfo.size);
                    int ret = mRtmpLive.StreamPusher(mBuffer.array(), mBufferInfo.size + ADTS_HEADER_SIZE,
                            timestamp, Config.MEDIA_TYPE_AUDIO);
                    Log.d(TAG, "Audio Push ret: " + ret);
                    if (Config.DumpOutput) {
                        Utils.dumpOutputBuffer(mBuffer.array(), 0, mBufferInfo.size + ADTS_HEADER_SIZE, mDumpOutputPath, true);
                    }
                    mMediaCodec.releaseOutputBuffer(index, false);
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mBuffers = mMediaCodec.getOutputBuffers();
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized (AudioStream.this) {
                        Log.v(TAG, "output format changed...");
                        //newFormat = mMediaCodec.getOutputFormat();
                    }
                } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    //Log.v(TAG, "No buffer available...");
                } else {
                    Log.e(TAG, "Message: " + index);
                }
            } while (mWriter != null);
        }
    }

    private void adts_write_frame_header(byte[] packet, int packetLen, int pce_size,
                                         int objecttype, int channel_conf) {
        byte kFieldId = 0;
        byte kMpegLayer = 0;
        byte kProtectionAbsense = 1;// 1: kAdtsHeaderLength = 7
        byte kPrivateStream = 0;
        byte kCopyright = 0;// 4 bits from originality to copyright start
        int kBufferFullness = 0x7FF;//VBR
        byte kFrameCount = 0;

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) (0xF0 | (kFieldId << 3) | (kMpegLayer << 1) | kProtectionAbsense);
        packet[2] = (byte) (((objecttype - 1) << 6) | (mSamplingRateIndex << 2) | (kPrivateStream << 1) | (channel_conf >> 2));
        packet[3] = (byte) (((channel_conf & 3) << 6) | (kCopyright << 2) | ((packetLen & 0x1800) >> 11));
        packet[4] = (byte) ((packetLen & 0x07F8) >> 3);
        packet[5] = (byte) (((packetLen & 0x07) << 5) | ((kBufferFullness & 0x07C0) >> 6));
        packet[6] = (byte) (((kBufferFullness & 0x03F) << 2) | kFrameCount);
    }

    public void stop() {
        try {
            Thread t = mThread;
            mThread = null;
            if (t != null) {
                t.interrupt();
                t.join();
            }
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        }
    }
}
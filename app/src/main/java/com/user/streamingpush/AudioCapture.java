package com.user.streamingpush;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by user0 on 2017/11/14.
 */

public class AudioCapture {
    private static final String TAG = "AudioCapture";

    private int mChannelConfig = 1;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int mSampleRate = 8000;
    private boolean mIsRecording = false;

    // Dump Output
    private String mDumpOutputPath;
    private static final boolean mDumpOutput = true;
    private static final String mDumpBasePath = Environment.getExternalStorageDirectory() + "/Movies/pcm_";

    private RecordTask mRecorder;
    private RtmpLive mRtmpLive;
    private AudioCodec mAudioCodec = new AudioCodec();

    public void startAudioRecord(RtmpLive rtmpLive) {
        Log.d(TAG, "startAudioRecord");
        mRecorder = new RecordTask();
        mRecorder.execute();
        this.mRtmpLive = rtmpLive;

        if (mDumpOutput) {
            SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = sTimeFormat.format(new Date());
            mDumpOutputPath = mDumpBasePath + mSampleRate + "_" + mChannelConfig
                    + "_" + mAudioEncoding + "_" + date + ".pcm";
        }
    }

    public void stopAudioRecord() {
        mIsRecording = false;
        mAudioCodec.onDestroy();
    }

    class RecordTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsRecording = true;
            Log.d(TAG, "AudioRecording");
            try {
                int bufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioEncoding);
                AudioRecord mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        mSampleRate, mChannelConfig, mAudioEncoding, bufferSize);
                byte[] buffer = new byte[bufferSize * 2];

                mAudioCodec.initAudioEncoder(mSampleRate, mChannelConfig, mAudioEncoding, mRtmpLive);
                mAudioRecord.startRecording();

                while (mIsRecording) {
                    int bufferReadResult = mAudioRecord.read(buffer, 0, buffer.length);
                    Log.d(TAG, "read sample bufferReadResult: " + bufferReadResult);
                    mAudioCodec.onPcmFrameEncoding(buffer);
                    Utils.dumpOutputBuffer(buffer, 0, bufferReadResult, mDumpOutputPath, true);
                }

                mAudioRecord.stop();
            } catch (Exception e) {
                // TODO: handle exception
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Void result) {

        }

    }

}

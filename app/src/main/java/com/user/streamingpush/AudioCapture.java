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

    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int mFrequence = 44100;
    private boolean mIsRecording = false;

    // Dump Output
    private String mDumpOutputPath;
    private static final boolean mDumpOutput = true;
    private static final String mDumpBasePath = Environment.getExternalStorageDirectory() + "/Movies/pcm_";

    private RecordTask mRecorder;

    public void startAudioRecord() {
        Log.d(TAG, "startAudioRecord");
        mRecorder = new RecordTask();
        mRecorder.execute();

        if (mDumpOutput) {
            SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = sTimeFormat.format(new Date());
            mDumpOutputPath = mDumpBasePath + mFrequence + "_" + mChannelConfig
                    + "_" + mAudioEncoding + "_" + date + ".pcm";
        }
    }

    public void stopAudioRecord() {
        mIsRecording = false;
    }

    class RecordTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsRecording = true;
            Log.d(TAG, "AudioRecording");
            try {
                int bufferSize = AudioRecord.getMinBufferSize(mFrequence, mChannelConfig, mAudioEncoding);
                AudioRecord mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        mFrequence, mChannelConfig, mAudioEncoding, bufferSize);
                byte[] buffer = new byte[bufferSize * 2];

                mAudioRecord.startRecording();

                while (mIsRecording) {
                    int bufferReadResult = mAudioRecord.read(buffer, 0, buffer.length);
                    Log.d(TAG, "read sample bufferReadResult: " + bufferReadResult);
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

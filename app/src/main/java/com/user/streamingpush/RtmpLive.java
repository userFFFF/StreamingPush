package com.user.streamingpush;

import android.util.Log;

/**
 * Created by user0 on 2017/11/2.
 */

public class RtmpLive {
    public static final String TAG = "StreamingPush-RtmpLive";
    private long mPusherObj;

    public interface onStreamingCallback {
        public void onCallbak(int code);
    }

    // Used to load the 'rtmplive-lib' library on application startup.
    static {
        System.loadLibrary("rtmplive-lib");
    }

    /**
     * A native method that is implemented by the 'rtmplive-lib' native library,
     * which is packaged with this application.
     */
    private native long Init(String url, onStreamingCallback callback);

    private native int PushStreaming(long pushObj, byte[] data, int size, long timestamp, int type);

    private native void Stop(long pushObj);

    public void InitPusher(String url, onStreamingCallback mCallback) {
        mPusherObj = Init(url, mCallback);
    }

    public int StreamPusher(byte[] data, int size, long timestamp, int type) {
        if (mPusherObj != 0) {
            return PushStreaming(mPusherObj, data, size, timestamp, type);
        } else {
            Log.e(TAG, "Push Handler Error!");
            return -1;
        }
    }

    public void StopPusher() {
        if (mPusherObj != 0) {
            Stop(mPusherObj);
            mPusherObj = 0;
        }
    }
}

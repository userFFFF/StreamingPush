package com.user.streamingpush;

/**
 * Created by user0 on 2017/11/2.
 */

public class RtmpLive {



    public interface onStreamingCallback {
        public void onCallbak(int code);
    }
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("rtmplive-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native long Init(String url, onStreamingCallback callback);
    public native int PushStreaming(long pushObj, byte[] data, int size, long timestamp, int type);
    public native void Stop(long pushObj);
}

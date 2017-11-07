package com.user.streamingpush;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "StreamingPush";
    RtmpLive rtmplive = new RtmpLive();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(rtmplive.stringFromJNI());
        rtmplive.Init("rtmp://video-center.alivecdn.com/live/livestream1?vhost=push.yangxudong.com", new RtmpLive.onStreamingCallback() {
            @Override
            public void onCallbak(int code) {
                Log.d(TAG, "code = " + code);
            }
        });
    }


}

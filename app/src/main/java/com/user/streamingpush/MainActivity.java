package com.user.streamingpush;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private Button startbtn;
    RtmpLive rtmplive = new RtmpLive();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        startbtn = findViewById(R.id.btn_start);
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            }
        });
        tv.setText(rtmplive.stringFromJNI());
        rtmplive.Init("rtmp://video-center.alivecdn.com/live/livestream?vhost=push.yangxudong.com", new RtmpLive.onStreamingCallback() {
            @Override
            public void onCallbak(int code) {
                Log.d(TAG, "code = " + code);
            }
        });
    }
}

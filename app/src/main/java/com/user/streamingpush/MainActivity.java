package com.user.streamingpush;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = "MainActivity";
    private Button startbtn;
    EditText mEditText;
    String mRtmpUrl;
    //AudioCapture mAudioCapture = new AudioCapture();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        mEditText = findViewById(R.id.EditTxt_URL);
        RadioGroup mRadioGroup_Resolution = findViewById(R.id.RadioGroup_Solution);
        RadioGroup mRadioGroup_FPS = findViewById(R.id.RadioGroup_FPS);
        mRadioGroup_Resolution.setOnCheckedChangeListener(this);
        mRadioGroup_FPS.setOnCheckedChangeListener(this);

        startbtn = findViewById(R.id.Btn_start);
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRtmpUrl = mEditText.getText().toString();
                Log.i(TAG, "rtmp URL" + mRtmpUrl);
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
                //mAudioCapture.startAudioRecord();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mAudioCapture.stopAudioRecord();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Log.d(TAG, "onCheckedChanged");
        switch (checkedId) {
            case R.id.Radio_10:
                Log.i(TAG, "10 fps");
                break;
            case R.id.Radio_15:
                Log.i(TAG, "15 fps");
                break;
            case R.id.Radio_20:
                Log.i(TAG, "20 fps");
                break;
            case R.id.Radio_25:
                Log.i(TAG, "25 fps");
                break;
            case R.id.Radio_30:
                Log.i(TAG, "30 fps");
                break;
            case R.id.Radio_240P:
                Log.i(TAG, "240P");
                break;
            case R.id.Radio_320P:
                Log.i(TAG, "320P");
                break;
            case R.id.Radio_480P:
                Log.i(TAG, "480P");
                break;
            case R.id.Radio_640P:
                Log.i(TAG, "640P");
                break;
            case R.id.Radio_720P:
                Log.i(TAG, "720P");
                break;
            case R.id.Radio_1080P:
                Log.i(TAG, "1080P");
                break;
            default:
                break;
        }
    }
}

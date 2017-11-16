package com.user.streamingpush;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = "MainActivity";
    private Button startbtn;
    EditText mEditText;
    String mRtmpUrl;

    SharedPreferences mSharedPre;
    SharedPreferences.Editor mEditor;
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
        mSharedPre = getSharedPreferences(Config.NAME, Activity.MODE_PRIVATE);
        mEditor = mSharedPre.edit();

        onResolutionChecked();
        onFPSSetChecked();

        String mURL = mSharedPre.getString(Config.SERVER_URL, "");
        mEditText.setText(mURL);

        startbtn = findViewById(R.id.Btn_start);
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRtmpUrl = mEditText.getText().toString();
                mEditor.putString(Config.SERVER_URL, mRtmpUrl);
                mEditor.commit();
                Log.i(TAG, "rtmp URL" + mRtmpUrl);
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
                //mAudioCapture.startAudioRecord();
            }
        });
    }

    private void onFPSSetChecked() {
        RadioButton mRadio_10 = findViewById(R.id.Radio_10);
        RadioButton mRadio_15 = findViewById(R.id.Radio_15);
        RadioButton mRadio_20 = findViewById(R.id.Radio_20);
        RadioButton mRadio_25 = findViewById(R.id.Radio_25);
        RadioButton mRadio_30 = findViewById(R.id.Radio_30);
        int mFPS = mSharedPre.getInt(Config.FPS, 15);

        switch (mFPS) {
            case 10:
                mRadio_10.setChecked(true);
                break;
            case 20:
                mRadio_20.setChecked(true);
                break;
            case 25:
                mRadio_25.setChecked(true);
                break;
            case 30:
                mRadio_30.setChecked(true);
                break;
            case 15:
            default:
                mRadio_15.setChecked(true);
                break;
        }
    }

    private void onResolutionChecked() {
        RadioButton mRadio_240P = findViewById(R.id.Radio_240P);
        RadioButton mRadio_320P = findViewById(R.id.Radio_320P);
        RadioButton mRadio_480P = findViewById(R.id.Radio_480P);
        RadioButton mRadio_640P = findViewById(R.id.Radio_640P);
        RadioButton mRadio_720P = findViewById(R.id.Radio_720P);
        RadioButton mRadio_1080P = findViewById(R.id.Radio_1080P);
        int mResolution = mSharedPre.getInt(Config.RESOLUTION, 480);

        switch (mResolution) {
            case 240:
                mRadio_240P.setChecked(true);
                break;
            case 320:
                mRadio_320P.setChecked(true);
                break;
            case 640:
                mRadio_640P.setChecked(true);
                break;
            case 720:
                mRadio_720P.setChecked(true);
                break;
            case 1080:
                mRadio_1080P.setChecked(true);
                break;
            case 480:
            default:
                mRadio_480P.setChecked(true);
                break;
        }
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
                mEditor.putInt(Config.FPS, 10);
                mEditor.commit();
                break;
            case R.id.Radio_15:
                Log.i(TAG, "15 fps");
                mEditor.putInt(Config.FPS, 15);
                mEditor.commit();
                break;
            case R.id.Radio_20:
                Log.i(TAG, "20 fps");
                mEditor.putInt(Config.FPS, 20);
                mEditor.commit();
                break;
            case R.id.Radio_25:
                Log.i(TAG, "25 fps");
                mEditor.putInt(Config.FPS, 25);
                mEditor.commit();
                break;
            case R.id.Radio_30:
                Log.i(TAG, "30 fps");
                mEditor.putInt(Config.FPS, 30);
                mEditor.commit();
                break;
            case R.id.Radio_240P:
                Log.i(TAG, "240P");
                mEditor.putInt(Config.RESOLUTION, 240);
                mEditor.commit();
                break;
            case R.id.Radio_320P:
                Log.i(TAG, "320P");
                mEditor.putInt(Config.RESOLUTION, 320);
                mEditor.commit();
                break;
            case R.id.Radio_480P:
                Log.i(TAG, "480P");
                mEditor.putInt(Config.RESOLUTION, 480);
                mEditor.commit();
                break;
            case R.id.Radio_640P:
                Log.i(TAG, "640P");
                mEditor.putInt(Config.RESOLUTION, 640);
                mEditor.commit();
                break;
            case R.id.Radio_720P:
                Log.i(TAG, "720P");
                mEditor.putInt(Config.RESOLUTION, 720);
                mEditor.commit();
                break;
            case R.id.Radio_1080P:
                Log.i(TAG, "1080P");
                mEditor.putInt(Config.RESOLUTION, 1080);
                mEditor.commit();
                break;
            default:
                break;
        }
    }
}

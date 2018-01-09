package com.user.streamingpush;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Toast;

import com.example.cloudmedia.CloudMedia;
import com.example.cloudmedia.LocalMediaNode;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = "MainActivity";
    private Button mLoginBtn;
    private Button mLogoutBtn;
    private EditText mEditText;
    private String mRtmpUrl;
    private String mLoginNickName;

    private boolean mOnline = false;
    private boolean mIsPushing = false;
    private int nRef = 0;

    private SharedPreferences mSharedPre;
    private SharedPreferences.Editor mEditor;

    private LocalMediaNode mLocalMediaNode;
    private CloudMedia mCloudMedia;

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

        mLoginBtn = findViewById(R.id.Btn_login);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoginNickName = mEditText.getText().toString();
                if (mLoginNickName == null) {
                    mLoginNickName = "USER0";
                }
                if (mOnline == false) {
                    if (mCloudMedia == null) {
                        mCloudMedia = new CloudMedia(getApplicationContext());
                        onCloudMediaUpdate();
                    } else {
                        mCloudMedia.connect(mLoginNickName, CloudMedia.CMRole.ROLE_PUSHER, new CloudMedia.RPCResultListener() {
                            @Override
                            public void onSuccess(String s) {
                                mOnline = true;
                                mLoginBtn.setEnabled(false);
                                mLogoutBtn.setEnabled(true);
                                Log.d(TAG, "connect onSuccess");
                            }

                            @Override
                            public void onFailure(String s) {
                                Log.d(TAG, "connect onFailure");
                            }
                        });
                    }
                }
            }
        });
        mLogoutBtn = findViewById(R.id.Btn_logout);
        mLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCloudMedia != null) {
                    mCloudMedia.disconnect();
                    mCloudMedia = null;

                    mOnline = false;
                    mLoginBtn.setEnabled(true);
                    mLogoutBtn.setEnabled(false);
                }
            }
        });
    }

    private void onCloudMediaUpdate() {
        mCloudMedia.connect(mLoginNickName, CloudMedia.CMRole.ROLE_PUSHER, new CloudMedia.RPCResultListener() {
            @Override
            public void onSuccess(String s) {
                mOnline = true;
                mLoginBtn.setEnabled(false);
                mLogoutBtn.setEnabled(true);
                Log.i(TAG, "connect onSuccess");
            }

            @Override
            public void onFailure(String s) {
                Log.i(TAG, "connect onFailure");
            }
        });

        mLocalMediaNode = mCloudMedia.declareLocalMediaNode();
        mLocalMediaNode.setOnStartPushMediaActor(new LocalMediaNode.OnStartPushMedia() {
            @Override
            public boolean onStartPushMedia(String params) {
                nRef++;
                Log.d(TAG, "onStartPushMedia nRef: " + nRef + ", params:" + nRef);
                if (mIsPushing == true) {
                    return true;
                }
                if (params == null) {
                    Toast.makeText(getApplicationContext(), "params is NULL!", Toast.LENGTH_SHORT);
                    return false;
                }

                try {
                    JSONObject mJSOONObj = new JSONObject(params);
                    mRtmpUrl = mJSOONObj.getString("url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mRtmpUrl == null) {
                    Toast.makeText(getApplicationContext(), "RTMP Server IP is NULL!", Toast.LENGTH_SHORT);
                    return false;
                }

                mEditor.putString(Config.SERVER_URL, mRtmpUrl);
                mEditor.commit();

                startActivity(new Intent(MainActivity.this, CameraActivity.class));

                mIsPushing = true;
                mCloudMedia.updateStreamStatus(CloudMedia.CMStreamStatus.PUSHING, new CloudMedia.RPCResultListener() {
                    @Override
                    public void onSuccess(String s) {
                        Log.d(TAG, "updateStreamStatus onSuccess");
                    }

                    @Override
                    public void onFailure(String s) {
                        Log.d(TAG, "updateStreamStatus onFailure");
                    }
                });
                return true;
            }
        });
        mLocalMediaNode.setOnStopPushMediaActor(new LocalMediaNode.OnStopPushMedia() {
            @Override
            public boolean onStopPushMedia(String params) {
                nRef--;
                Log.d(TAG, "onStopPushMedia nRef: " + nRef);
                if (nRef == 0) {
                    getApplicationContext().sendBroadcast(new Intent("finish"));
                    mIsPushing = false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        mIsPushing = false;
        nRef = 0;
        if (mCloudMedia != null) {
            mCloudMedia.updateStreamStatus(CloudMedia.CMStreamStatus.PUSHING_CLOSE, new CloudMedia.RPCResultListener() {
                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "updateStreamStatus onSuccess");
                }

                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "updateStreamStatus onFailure");
                }
            });
        }
        super.onResume();
    }

    private void onFPSSetChecked() {
        RadioButton mRadio_10 = findViewById(R.id.Radio_10);
        RadioButton mRadio_15 = findViewById(R.id.Radio_15);
        RadioButton mRadio_20 = findViewById(R.id.Radio_20);
        RadioButton mRadio_25 = findViewById(R.id.Radio_25);
        RadioButton mRadio_30 = findViewById(R.id.Radio_30);
        int mFPS = mSharedPre.getInt(Config.FPS, Config.PFS_10);

        switch (mFPS) {
            case Config.PFS_10:
                mRadio_10.setChecked(true);
                break;
            case Config.PFS_20:
                mRadio_20.setChecked(true);
                break;
            case Config.PFS_25:
                mRadio_25.setChecked(true);
                break;
            case Config.PFS_30:
                mRadio_30.setChecked(true);
                break;
            case Config.PFS_15:
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
        int mResolution = mSharedPre.getInt(Config.RESOLUTION, Config.Resolution_240P);

        switch (mResolution) {
            case Config.Resolution_240P:
                mRadio_240P.setChecked(true);
                break;
            case Config.Resolution_320P:
                mRadio_320P.setChecked(true);
                break;
            case Config.Resolution_640P:
                mRadio_640P.setChecked(true);
                break;
            case Config.Resolution_720P:
                mRadio_720P.setChecked(true);
                break;
            case Config.Resolution_1080P:
                mRadio_1080P.setChecked(true);
                break;
            case Config.Resolution_480P:
            default:
                mRadio_480P.setChecked(true);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCloudMedia != null) {
            mCloudMedia.disconnect();

            mOnline = false;
            mLoginBtn.setEnabled(true);
            mLogoutBtn.setEnabled(false);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Log.d(TAG, "onCheckedChanged");

        switch (checkedId) {
            case R.id.Radio_10:
                Log.i(TAG, "10 fps");
                mEditor.putInt(Config.FPS, Config.PFS_10);
                mEditor.commit();
                break;
            case R.id.Radio_15:
                Log.i(TAG, "15 fps");
                mEditor.putInt(Config.FPS, Config.PFS_15);
                mEditor.commit();
                break;
            case R.id.Radio_20:
                Log.i(TAG, "20 fps");
                mEditor.putInt(Config.FPS, Config.PFS_20);
                mEditor.commit();
                break;
            case R.id.Radio_25:
                Log.i(TAG, "25 fps");
                mEditor.putInt(Config.FPS, Config.PFS_25);
                mEditor.commit();
                break;
            case R.id.Radio_30:
                Log.i(TAG, "30 fps");
                mEditor.putInt(Config.FPS, Config.PFS_30);
                mEditor.commit();
                break;
            case R.id.Radio_240P:
                Log.i(TAG, "240P");
                mEditor.putInt(Config.RESOLUTION, Config.Resolution_240P);
                mEditor.commit();
                break;
            case R.id.Radio_320P:
                Log.i(TAG, "320P");
                mEditor.putInt(Config.RESOLUTION, Config.Resolution_320P);
                mEditor.commit();
                break;
            case R.id.Radio_480P:
                Log.i(TAG, "480P");
                mEditor.putInt(Config.RESOLUTION, Config.Resolution_480P);
                mEditor.commit();
                break;
            case R.id.Radio_640P:
                Log.i(TAG, "640P");
                mEditor.putInt(Config.RESOLUTION, Config.Resolution_640P);
                mEditor.commit();
                break;
            case R.id.Radio_720P:
                Log.i(TAG, "720P");
                mEditor.putInt(Config.RESOLUTION, Config.Resolution_720P);
                mEditor.commit();
                break;
            case R.id.Radio_1080P:
                Log.i(TAG, "1080P");
                mEditor.putInt(Config.RESOLUTION, Config.Resolution_1080P);
                mEditor.commit();
                break;
            default:
                break;
        }
    }
}
package com.user.streamingpush;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cmteam.cloudmedia.PushNode;
import com.cmteam.cloudmedia.CloudMedia;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = "MainActivity";
    private View mLoginLayout;
    private View mLoginedLayout;
    private Button mLoginBtn;
    private EditText mUserEt;
    private EditText mPswEt;
    private String mRtmpUrl;
    private String mLoginNickName;

    private boolean mOnline = false;
    private boolean mIsPushing = false;

    private SharedPreferences mSharedPre;
    private SharedPreferences.Editor mEditor;

    private final static String IP = "139.224.128.15";//"192.168.199.68";//
    private final static String PORT = "8085";
    private final static int MSG_SIGNIN_RESULT = 0;
    private CloudMedia mCloudMedia;
    private PushNode mPushNode;
    private static boolean mIsSignin = false;
    private CustomDialog mWaitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(mNetworkConnectChangedReceiver, filter);

        mLoginLayout = findViewById(R.id.login);
        mLoginedLayout = findViewById(R.id.logined);

        // Example of a call to a native method
        mUserEt = findViewById(R.id.EditTxt_name);
        mUserEt.setText("A352686");//A113777
        mPswEt = findViewById(R.id.EditTxt_psw);
        mPswEt.setText("1234567890");
        RadioGroup mRadioGroup_Resolution = findViewById(R.id.RadioGroup_Solution);
        RadioGroup mRadioGroup_FPS = findViewById(R.id.RadioGroup_FPS);
        mRadioGroup_Resolution.setOnCheckedChangeListener(this);
        mRadioGroup_FPS.setOnCheckedChangeListener(this);
        mSharedPre = getSharedPreferences(Config.NAME, Activity.MODE_PRIVATE);
        mEditor = mSharedPre.edit();
        onResolutionChecked();
        onFPSSetChecked();

        mLoginBtn = findViewById(R.id.signinbtn);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsSignin) {
                    showWaitingDialog();
                    mLoginNickName = mUserEt.getText().toString();
                    if (mLoginNickName == null || mLoginNickName.length() == 0) {
                        mLoginNickName = "USER0";
                    }
                    if (!mOnline) {
                        signin();
                    }
                } else {
                    signout();
                    mLoginLayout.setVisibility(View.VISIBLE);
                    mLoginedLayout.setVisibility(View.GONE);
                    mLoginBtn.setBackgroundResource(R.drawable.signinbtnbg);
                    mLoginBtn.setText(R.string.signin_btn);
                }
                mIsSignin = !mIsSignin;
            }
        });
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SIGNIN_RESULT:
                    if ((boolean)msg.obj) {
                        connectCloudMedia();
                    }else {
                        if (mWaitDialog.isShowing())
                            dismissWaitDialog();
                        Toast.makeText(MainActivity.this, R.string.signin_failed, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void signin() {
        new Thread() {
            @Override
            public void run() {
                if (mCloudMedia == null) {
                    mCloudMedia = CloudMedia.get();
                }
                boolean loginsuccess = mCloudMedia.login(IP, PORT, mLoginNickName, mPswEt.getText().toString());
                Log.d(TAG, "get loginresult:"+loginsuccess);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SIGNIN_RESULT, loginsuccess));
            }
        }.start();
    }

    private void signout() {
        if (mPushNode != null) {
            mPushNode.disconnect();
            mPushNode = null;
        }
        if (mCloudMedia != null && mOnline) {
            new Thread() {
                @Override
                public void run() {
                    mCloudMedia.logout(mLoginNickName);
                    mOnline = false;
                }
            }.start();
        }
    }

    private void showWaitingDialog() {
        if (mWaitDialog != null) {
            if (mWaitDialog.isShowing())
                return;
            mWaitDialog.show();
            return;
        }
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View waitView = inflater.inflate(R.layout.waiting_dialog, null);
        mWaitDialog = new CustomDialog.Builder(this)
                .create(waitView, R.style.CustomDialog, Gravity.CENTER);
        ImageView images = waitView.findViewById(R.id.images);
        ((Animatable)images.getDrawable()).start();
        mWaitDialog.setDialogOnKeyDownListner(new CustomDialog.DialogOnKeyDownListner() {
            @Override
            public void onKeyDownListener(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Log.d(TAG, "Dialog back key down");
                    dismissWaitDialog();
                }
            }
        });
        mWaitDialog.show();
    }

    private void dismissWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    private void connectCloudMedia() {
        if(mPushNode == null) {
            mPushNode = mCloudMedia.declarePushNode(getApplicationContext(), mLoginNickName, "default");
        }
        mPushNode.setOnStartPushMediaActor(new PushNode.OnStartPushMedia() {
            @Override
            public boolean onStartPushMedia(String params) {

                Log.d(TAG, "onStartPushMedia params: " + params);
                if (mIsPushing == true) {
                    return true;
                }
                if (params == null) {
                    Toast.makeText(getApplicationContext(), "params is NULL!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    JSONObject mJSOONObj = new JSONObject(params);
                    mRtmpUrl = mJSOONObj.getString("url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mRtmpUrl == null) {
                    Toast.makeText(getApplicationContext(), "RTMP Server IP is NULL!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                mEditor.putString(Config.SERVER_URL, mRtmpUrl);
                mEditor.commit();

                startActivity(new Intent(MainActivity.this, CameraActivity.class));

                mIsPushing = true;
                mPushNode.updateStreamStatus(CloudMedia.CMStreamStatus.PUSHING, new CloudMedia.RPCResultListener() {
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
        mPushNode.setOnStopPushMediaActor(new PushNode.OnStopPushMedia() {
            @Override
            public boolean onStopPushMedia(String s) {
                getApplicationContext().sendBroadcast(new Intent("finish"));
                mIsPushing = false;
                return true;
            }
        });
        mPushNode.connect(mCloudMedia.getUser(mLoginNickName), new CloudMedia.RPCResultListener() {
            @Override
            public void onSuccess(String s) {
                mOnline = true;
                Log.d(TAG, "connect onSuccess");
                if (mWaitDialog.isShowing())
                    dismissWaitDialog();

                EditText nameET = findViewById(R.id.name);
                EditText pswET = findViewById(R.id.psw);
                nameET.setText(mLoginNickName);
                pswET.setText(mPswEt.getText());

                TextView solutionTX = findViewById(R.id.solution);
                TextView fpsTX = findViewById(R.id.fps);

                int resolution = mSharedPre.getInt(Config.RESOLUTION, Config.Resolution_480P);
                int fps = mSharedPre.getInt(Config.FPS, Config.PFS_20);
                solutionTX.setText(getResources().getText(R.string.txt_Reslt)+String.valueOf(resolution));
                fpsTX.setText(getResources().getText(R.string.txt_Fps)+String.valueOf(fps));

                mLoginLayout.setVisibility(View.GONE);
                mLoginedLayout.setVisibility(View.VISIBLE);
                mLoginBtn.setBackgroundResource(R.drawable.signoutbtnbg);
                mLoginBtn.setText(R.string.signout_btn);

                mPushNode.setMessageListener(new CloudMedia.OnMessageListener() {
                    @Override
                    public void onMessage(String s, String s1, String s2) {
                        Log.d(TAG, "onMessage:s = "+s+", s1 = "+s1+", s2 ="+s2);
                        mPushNode.sendMessage(s, s1, "pusher: receied");
                    }
                });
            }

            @Override
            public void onFailure(String s) {
                Log.d(TAG, "connect onFailure");
                if (mWaitDialog.isShowing())
                    dismissWaitDialog();
                Toast.makeText(getApplicationContext(), "Login failure, please checked the network.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        mIsPushing = false;
        if (mPushNode != null) {
            if (Config.RTMP_PUSH_STATE_ERROR == mSharedPre.getInt(Config.RTMP_STATE, Config.RTMP_PUSH_STATE_STOPPED)) {
                mEditor.putInt(Config.RTMP_STATE, Config.RTMP_PUSH_STATE_STOPPED);
                mEditor.commit();
                mPushNode.updateStreamStatus(CloudMedia.CMStreamStatus.PUSHING_ERROR, new CloudMedia.RPCResultListener() {
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
            mPushNode.updateStreamStatus(CloudMedia.CMStreamStatus.PUSHING_CLOSE, new CloudMedia.RPCResultListener() {
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
        int mFPS = mSharedPre.getInt(Config.FPS, Config.PFS_20);

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
        int mResolution = mSharedPre.getInt(Config.RESOLUTION, Config.Resolution_480P);

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
        signout();
        unregisterReceiver(mNetworkConnectChangedReceiver);
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

    private BroadcastReceiver mNetworkConnectChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                Log.d(TAG, "wifiState: " + wifiState);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        break;
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        break;
                    default:
                        break;
                }
            }

            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                    Log.e(TAG, "isConnected: " + isConnected);
                    if (isConnected) {
                    } else {
                    }
                }
            }

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager manager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                Log.i(TAG, "CONNECTIVITY_ACTION");

                NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
                if (activeNetwork != null) { // connected to the internet
                    if (activeNetwork.isConnected()) {
                        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                            // connected to wifi
                            Log.e(TAG, "当前WiFi连接可用 ");
                        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                            // connected to the mobile provider's data plan
                            Log.e(TAG, "当前移动网络连接可用 ");
                        }
                    } else {
                        Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
                        getApplicationContext().sendBroadcast(new Intent("finish"));
                    }
                } else {   // not connected to the internet
                    getApplicationContext().sendBroadcast(new Intent("finish"));
                    Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
                }
            }
        }
    };
}
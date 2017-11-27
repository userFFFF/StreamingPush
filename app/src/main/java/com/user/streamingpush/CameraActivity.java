package com.user.streamingpush;

/**
 * Created by user0 on 2017/11/8.
 */

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private String TAG = "CameraActivity";

    // Camera
    private Camera mCamera;
    private int mCameraNum;
    private SurfaceTexture mSurface;
    private int cameraRotationOffset;
    //private Camera.Size mSize;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    //View
    private Button mSwitchCamButton;
    private Button mQuitLive;
    private TextureView mTextureView;

    //Video Codec
    private VideoCodec mVideoCode = new VideoCodec();

    //Parameter
    private int mWidth = 640, mHeight = 480;
    private int framerate = 20;
    private int bitrate = 2 * mWidth * mHeight * framerate / 20;

    //SharedPreferences
    SharedPreferences mSharedPre;
    SharedPreferences.Editor mEditor;

    //RTMP Live
    RtmpLive mRtmpLive = new RtmpLive();

    //Audio
    private AudioStream mAudioStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mSharedPre = getSharedPreferences(Config.NAME, Activity.MODE_PRIVATE);
        mEditor = mSharedPre.edit();
        String mURL = mSharedPre.getString(Config.SERVER_URL, "");
        onFPSConfig();
        onResolutionConfig();

        Log.d(TAG, "mURL: " + mURL + " framerate: " + framerate + " resolution: " + mWidth + "x" + mHeight);
        if (mURL == null) {
            Log.e(TAG, "NULL SERVER URL!!!!!!!!!");
            return;
        }

        mRtmpLive.InitPusher(mURL, new RtmpLive.onStreamingCallback() {
            @Override
            public void onCallbak(int code) {
                switch (code) {
                    case CODE.RTMP_STREAM_SIZE_ERR:
                    case CODE.RTMP_HANDLER_ERR:
                    case CODE.RTMP_STREAM_UNSUPPORT:
                    case CODE.RTMP_STREAM_ERR:
                    case CODE.RTMP_CONNECT_ERR:
                    case CODE.RTMP_HANDSHAKE_ERR:
                    case CODE.RTMP_CREATE_ERR:
                    case CODE.RTMP_STATE_SUCCESS:
                    case CODE.RTMP_STATE_CONNECTED:
                    case CODE.RTMP_STATE_STOPED:
                        break;
                }
                Log.d(TAG, "code = " + code);
            }
        });

        bitrate = (int) (mWidth * mHeight * framerate * 2 * 0.05f);
        if (mWidth >= 1920 || mHeight >= 1920) {
            bitrate *= 0.3;
        } else if (mWidth >= 1280 || mHeight >= 1280) {
            bitrate *= 0.4;
        } else if (mWidth >= 720 || mHeight >= 720) {
            bitrate *= 0.6;
        }

        mAudioStream = new AudioStream(mRtmpLive);
        mAudioStream.startRecord();
        mTextureView = findViewById(R.id.camera_preview);
        mTextureView.setSurfaceTextureListener(this);

        mSwitchCamButton = findViewById(R.id.Btn_switchCam);
        mSwitchCamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        mQuitLive = findViewById(R.id.Btn_quitlive);
        mQuitLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
        destroyCamera();
        mAudioStream.stop();
        mVideoCode.onDestroy();
        mRtmpLive.StopPusher();

    }

    private void onFPSConfig() {
        framerate = mSharedPre.getInt(Config.FPS, Config.PFS_15);
    }

    private void onResolutionConfig() {
        int mResolution = mSharedPre.getInt(Config.RESOLUTION, Config.Resolution_480P);
        switch (mResolution) {
            case Config.Resolution_240P:
                mWidth = 320;
                mHeight = 240;
                break;
            case Config.Resolution_320P:
                mWidth = 480;
                mHeight = 320;
                break;
            case Config.Resolution_480P:
                mWidth = 640;
                mHeight = 480;
                break;
            case Config.Resolution_640P:
                mWidth = 720;
                mHeight = 640;
                break;
            case Config.Resolution_720P:
                mWidth = 1280;
                mHeight = 720;
                break;
            case Config.Resolution_1080P:
                mWidth = 1920;
                mHeight = 1080;
                break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: size:" + width + "x" + height);
        mSurface = surface;
        createCamera(surface);
        startPreview();
        mVideoCode.initVideoEncoder(mWidth, mHeight, getDegree(), framerate, bitrate, mRtmpLive);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: size:" + width + "x" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        stopPreview();
        destroyCamera();
        mVideoCode.onDestroy();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private boolean createCamera(SurfaceTexture surface) {
        try {
            mCameraNum = Camera.getNumberOfCameras();
            Camera.CameraInfo info = new Camera.CameraInfo();
            Log.d(TAG, "mCameraNum = " + mCameraNum);
            for (int i = 0; i < mCameraNum; i++) {
                Camera.getCameraInfo(i, info);
                //
                if (info.facing == mCameraId) {
                    mCamera = Camera.open(i);
                    break;
                }
            }
            if (mCamera == null) {
                Log.d(TAG, "No front-facing camera found; opening default");
                mCamera = Camera.open();    // opens first back-facing camera
            }
            if (mCamera == null) {
                throw new RuntimeException("Unable to open camera");
            }
            Camera.Parameters mCameraParamters = mCamera.getParameters();
            int[] max = determineMaximumSupportedFramerate(mCameraParamters);
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, camInfo);

            cameraRotationOffset = camInfo.orientation;
            Log.d(TAG, "cameraRotationOffset: " + cameraRotationOffset);
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraRotationOffset += 180;
            }
            int rotate = (360 + cameraRotationOffset - getDegree()) % 360;
            mCameraParamters.setRotation(rotate);
            mCameraParamters.setPreviewFormat(mCameraParamters.getPreviewFormat());
//            List<Camera.Size> sizes = mCameraParamters.getSupportedPreviewSizes();
//            for (int i = 0; i < sizes.size(); i++) {
//                Camera.Size s = sizes.get(i);
//                Log.i(TAG, "Size: " + s.height + "x" + s.width);
//                if (mSize == null) {
//                    if (s.height == width && s.width == height) {
//                        mSize = s;
//                        break;
//                    }
//                }
//            }
//            if (sizes == null) {
//                mSize.width = width;
//                mSize.height = height;
//            }
            mCameraParamters.setPreviewSize(mWidth, mHeight);
            mCameraParamters.setPreviewFpsRange(max[0], max[1]);
            mCamera.setParameters(mCameraParamters);
            mCamera.autoFocus(null);
            int displayRotation = (cameraRotationOffset - getDegree() + 360) % 360;
            mCamera.setDisplayOrientation(displayRotation);
            mCamera.setPreviewTexture(surface);
            return true;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stack = sw.toString();
            Toast.makeText(this, stack, Toast.LENGTH_LONG).show();
            destroyCamera();
            e.printStackTrace();
            return false;
        }
    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
            int previewFormat = mCamera.getParameters().getPreviewFormat();
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            int size = previewSize.width * previewSize.height
                    * ImageFormat.getBitsPerPixel(previewFormat) / 8;
            mCamera.addCallbackBuffer(new byte[size]);
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
        }
    }

    public synchronized void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
        }
    }

    protected synchronized void destroyCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            try {
                mCamera.release();
            } catch (Exception e) {

            }
            mCamera = null;
        }
    }

    public static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters) {
        int[] maxFps = new int[]{0, 0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext(); ) {
            int[] interval = it.next();
            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }
        return maxFps;
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //Log.i(TAG, "onPreviewFrame cameraRotationOffset: " + cameraRotationOffset);
            byte dst[];
            if (cameraRotationOffset / 180 != 0) { // TODO FRONT
                byte[] rotation = Utils.rotateYUV420Degree180(data, mWidth, mHeight);
                dst = mVideoCode.onPreviewFrameEncoding(rotation);
            } else {
                dst = mVideoCode.onPreviewFrameEncoding(data);
            }
            camera.addCallbackBuffer(dst);
        }
    };

    public void switchCamera() {
        if (mCameraNum > 1) {
            stopPreview();
            destroyCamera();
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            createCamera(mSurface);
            startPreview();
            Log.i(TAG, "Camera has switched!");
        } else {
            Log.i(TAG, "This device does not support switch camera!");
        }
    }

    protected int getDegree() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        return degrees;
    }
}
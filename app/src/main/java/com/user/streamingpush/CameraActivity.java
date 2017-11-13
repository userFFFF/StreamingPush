package com.user.streamingpush;

/**
 * Created by user0 on 2017/11/8.
 */

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

import java.io.IOException;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private String TAG = "CameraActivity";

    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    ;
    private Camera.Parameters mCameraParamters;
    private Camera.Size mSize = null;
    private TextureView mTextureView;
    private Button mSwitchCamButton;
    private VideoCodec mVideoCode = new VideoCodec();
    private int mCameraNum;
    private CameraPreviewCallback mCameraPreviewCallback = new CameraPreviewCallback();
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 480;
    private byte[] mFrameCallbackBuffer = new byte[FRAME_WIDTH * FRAME_HEIGHT * 3 / 2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mTextureView = findViewById(R.id.camera_preview);
        mTextureView.setSurfaceTextureListener(this);

        mSwitchCamButton = findViewById(R.id.btn_switchCam);
        mSwitchCamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        doOpenCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
        doStopCamera();
        mVideoCode.onDestroy();

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: size:" + width + "x" + height);
        doStartPreview(surface, 20);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: size:" + width + "x" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void doOpenCamera() {
        Log.d(TAG, "Camera open...");
        mCameraNum = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < mCameraNum; i++) {
            Camera.getCameraInfo(i, info);
            //
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
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
        Log.d(TAG, "Camera opened...");
        mVideoCode.initVideoEncoder(FRAME_WIDTH, FRAME_HEIGHT, getDegree(), 15, 250000);
    }

    public void doStartPreview(SurfaceTexture surface, float previewRate) {
        Log.d(TAG, "doStartPreview");

        try {
            mCamera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initCamera();
    }

    public void doStopCamera() {
        Log.d(TAG, "doStopCamera");
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void initCamera() {
        Log.d(TAG, "initCamera");
        if (mCamera != null) {
            mCameraParamters = mCamera.getParameters();
            mCameraParamters.setPreviewFormat(mCameraParamters.getPreviewFormat());
            mCameraParamters.setFlashMode("off");
            mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

            List<Camera.Size> sizes = mCameraParamters.getSupportedPreviewSizes();
            for (int i = 0; i < sizes.size(); i++) {
                Camera.Size s = sizes.get(i);
                Log.i(TAG, "Size: " + s.height + "x" + s.width);
                if (mSize == null) {
                    if (s.height == FRAME_HEIGHT && s.width == FRAME_WIDTH) {
                        mSize = s;
                        break;
                    }
                }
            }
            if (sizes == null) {
                mSize.width = 640;
                mSize.height = 480;
            }
            mCameraParamters.setPreviewSize(mSize.width, mSize.height);
            Log.d(TAG, "mSize: " + mSize.width + "x" + mSize.height);

            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, camInfo);
            int rotation = (360 + camInfo.orientation - getDegree()) % 360;
            mCamera.setDisplayOrientation(rotation);
            mCamera.addCallbackBuffer(mFrameCallbackBuffer);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            List<String> focusModes = mCameraParamters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                mCameraParamters
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else {
                mCameraParamters
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(mCameraParamters);
            mCamera.startPreview();
        } else {
            Log.e(TAG, "mCamera is NULL");
        }
    }

    public void switchCamera() {
        if (mCameraNum > 1) {
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

    class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.i(TAG, "onPreviewFrame");
            byte dst[];
            dst = mVideoCode.onPreviewFrameEncoding(data);
            //long startTime = System.currentTimeMillis();
            //long endTime = System.currentTimeMillis();
            camera.addCallbackBuffer(dst);
        }
    }
}
package com.user.streamingpush;

/**
 * Created by user0 on 2017/11/8.
 */


import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.List;


public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{
    private String TAG = "CameraActivity";
    public static final String STORAGE_PATH = Environment.getExternalStorageDirectory().toString();

    private Camera mCamera;
    private Camera.Parameters mCameraParamters;
    private TextureView mTextureView;
    private Button mSwitchCamButton;
    private int mCameraNum;
    private CameraPreviewCallback mCameraPreviewCallback = new CameraPreviewCallback();
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 352;
    private byte[] mFrameCallbackBuffer = new byte[FRAME_WIDTH * FRAME_HEIGHT * 3 / 2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mTextureView = (TextureView) findViewById(R.id.camera_preview);
        mSwitchCamButton = (Button) findViewById(R.id.btn_switchCam);
        mSwitchCamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
        mTextureView.setSurfaceTextureListener(this);
        doOpenCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    protected void onDestroy() {
        doStopCamera();
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable: size:" + width +"," + height);
        doStartPreview(surface, 20);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void doOpenCamera() {
        Log.i(TAG, "Camera open....");
        mCameraNum = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < mCameraNum; i++) {
            Camera.getCameraInfo(i, info);
            //
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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
        Log.i(TAG, "Camera open over....");
        initCamera();
    }

    public void doStartPreview(SurfaceTexture surface, float previewRate) {
        Log.i(TAG, "doStartPreview()");

        try {
            mCamera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initCamera();
    }

    public void doStopCamera() {
        Log.i(TAG, "doStopCamera");
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            this.mCamera.release();
            this.mCamera = null;
        }
    }

    private void initCamera() {
        if (mCamera != null) {
            mCameraParamters = mCamera.getParameters();
            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            mCameraParamters.setFlashMode("off");
            mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            mCameraParamters.setPreviewSize(FRAME_WIDTH, FRAME_HEIGHT);
            mCamera.setDisplayOrientation(90);
            mCamera.addCallbackBuffer(mFrameCallbackBuffer);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            List<String> focusModes = this.mCameraParamters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                mCameraParamters
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setParameters(mCameraParamters);
            mCamera.startPreview();
        }
    }

    public void switchCamera(){
        if (mCameraNum > 1){
            Log.i(TAG, "Camera has switched！");
        }else {
            Log.i(TAG, "This device does not support switch camera");
        }
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.i(TAG, "onPreviewFrame");
            long startTime = System.currentTimeMillis();
            long endTime = System.currentTimeMillis();
            Log.i(TAG, Integer.toString((int)(endTime-startTime)) + "ms");
            camera.addCallbackBuffer(data);
        }
    }
}
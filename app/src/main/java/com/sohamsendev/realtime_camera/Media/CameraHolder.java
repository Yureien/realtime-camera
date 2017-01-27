package com.sohamsendev.realtime_camera.media;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

/**
 * This one uses the old, deprecated Camera API, for API 21-.
 * For now, the default one.
 * TODO: Build the Camera2 API.
 */

public class CameraHolder extends SurfaceView implements SurfaceHolder.Callback {

    public static interface CameraReadyCallback {
        public void onCameraReady();
    }

    private SurfaceHolder mHolder;
    private Camera mCamera;
    String TAG = "CameraHolder";

    private List<int[]> supportedFrameRate;
    private List<Camera.Size> supportedSizes;
    private Camera.Size currentSize;
    CameraReadyCallback cameraReadyCallback = null;

    public CameraHolder(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public List<Camera.Size> getSupportedSizes() {
        return supportedSizes;
    }

    public int previewWidth() {
        return currentSize.width;
    }

    public int previewHeight() {
        return currentSize.height;
    }

    public void setCameraReadyCallback(CameraReadyCallback cb) {
        cameraReadyCallback = cb;
    }

    public void startPreview() {
        if (mCamera != null)
            mCamera.startPreview();
    }

    public void stopPreview() {
        if (mCamera != null)
            mCamera.stopPreview();
    }

    public void setAutoFocus() {
        mCamera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean start, Camera camera) {
                //Do something...
            }
        });
    }

    public void release() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setupCamera(int wid, int hei, int bufNumber, double fps, Camera.PreviewCallback cb) {
        double diff = Math.abs(supportedSizes.get(0).width*supportedSizes.get(0).height - wid*hei);
        int targetIndex = 0;
        for(int i = 1; i < supportedSizes.size(); i++) {
            double newDiff =  Math.abs(supportedSizes.get(i).width*supportedSizes.get(i).height - wid*hei);
            if ( newDiff < diff) {
                diff = newDiff;
                targetIndex = i;
            }
        }
        currentSize.width = supportedSizes.get(targetIndex).width;
        currentSize.height = supportedSizes.get(targetIndex).height;

        diff = Math.abs(supportedFrameRate.get(0)[0] * supportedFrameRate.get(0)[1]  - fps*fps*1000*1000);
        targetIndex = 0;
        for(int i = 1; i < supportedFrameRate.size(); i++) {
            double newDiff = Math.abs(supportedFrameRate.get(i)[0] * supportedFrameRate.get(i)[1]  - fps*fps*1000*1000);
            if ( newDiff < diff) {
                diff = newDiff;
                targetIndex = i;
            }
        }
        int targetMaxFrameRate = supportedFrameRate.get(targetIndex)[0];
        int targetMinFrameRate = supportedFrameRate.get(targetIndex)[1];

        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewSize(currentSize.width, currentSize.height);
        p.setPreviewFormat(ImageFormat.NV21);
        p.setPreviewFpsRange(targetMaxFrameRate, targetMinFrameRate);
        mCamera.setParameters(p);

        PixelFormat pixelFormat = new PixelFormat();
        PixelFormat.getPixelFormatInfo(ImageFormat.NV21, pixelFormat);
        int bufSize = currentSize.width * currentSize.height * pixelFormat.bitsPerPixel / 8;
        byte[] buffer = null;
        for(int i = 0; i < bufNumber; i++) {
            buffer = new byte[ bufSize ];
            mCamera.addCallbackBuffer(buffer);
        }
        mCamera.setPreviewCallbackWithBuffer(cb);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
        if (cameraReadyCallback != null)
            cameraReadyCallback.onCameraReady();
    }

    private void initCamera() {
        mCamera = Camera.open();
        currentSize = mCamera.new Size(0, 0);

        Camera.Parameters parameters = mCamera.getParameters();
        supportedFrameRate = parameters.getSupportedPreviewFpsRange();
        supportedSizes = parameters.getSupportedPreviewSizes();
        currentSize = supportedSizes.get(supportedSizes.size() / 2);
        parameters.setPreviewSize(currentSize.width, currentSize.height);
        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch ( Exception ex) {
            ex.printStackTrace();
        }
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        release();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }
}
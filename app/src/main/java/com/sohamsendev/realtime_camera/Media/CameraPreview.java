package com.sohamsendev.realtime_camera.Media;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.sohamsendev.realtime_camera.Net.StreamServer;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private final int previewHeight = 720, previewWidth = 1280;
    private Camera camera;
    private StreamServer server;

    public CameraPreview(Context context) {
        super(context);
        server = new StreamServer(8081);
        server.start();
        getHolder().addCallback(this);
    }

    private native byte[] NV21toRGBA(byte[] in, int previewWidth, int previewHeight);

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open(0);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPreviewSize(previewWidth, previewHeight);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(null);
            camera.setPreviewCallback(this);
        } catch (IOException e) {
            Log.i(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (holder.getSurface() == null) {
            Log.i(TAG, "No proper holder");
            return;
        }
        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.i(TAG, "tried to stop a non-existent preview");
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(previewWidth, previewHeight);
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            byte[] newData = NV21toRGBA(data, previewWidth, previewHeight);
            server.addBufferToStack(newData);
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!server.isRunning()) server.stop();
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            releaseCamera();
        }
    }

    public void stopServer() {
        if (!server.isRunning()) server.stop();
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            releaseCamera();
        }
    }
}
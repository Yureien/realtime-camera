package com.sohamsendev.realtime_camera.Utils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.sohamsendev.realtime_camera.Activities.MainActivity;
import com.sohamsendev.realtime_camera.R;

public class ServerManager {

    private FragmentManager fragmentManager;
    private Fragment cameraPreview, controlPanel;
    private int connectedClients = 0;

    public ServerManager(FragmentManager manager) {
        fragmentManager = manager;
        controlPanel = MainActivity.controlPanel;
        cameraPreview = MainActivity.cameraPreview;
    }

    public void startCameraServer() {
        if (!cameraPreview.isAdded() && connectedClients == 0)
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, cameraPreview)
                    .hide(controlPanel)
                    .show(cameraPreview)
                    .commit();
        connectedClients++;
    }

    public void stopCameraServer() {
        if (connectedClients == 1) {
            fragmentManager.beginTransaction()
                    .remove(cameraPreview)
                    .show(controlPanel)
                    .commit();
        }
        connectedClients--;
    }

    public boolean isCameraServerRunning() {
        return connectedClients > 0;
    }
}

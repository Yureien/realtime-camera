package com.sohamsendev.realtime_camera.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.sohamsendev.realtime_camera.Media.CameraPreview;
import com.sohamsendev.realtime_camera.R;

public class CameraPreviewFragment extends Fragment {

    private CameraPreview preview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_preview, container, false);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FrameLayout cameraContainer = (FrameLayout)
                getView().findViewById(R.id.camera_preview_container);
        preview = new CameraPreview(getContext());
        cameraContainer.addView(preview);
    }

    @Override
    public void onDestroyView() {
        preview.stopServer();
        preview = null;
        super.onDestroyView();
    }
}
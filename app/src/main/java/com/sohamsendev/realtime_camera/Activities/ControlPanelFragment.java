package com.sohamsendev.realtime_camera.Activities;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.sohamsendev.realtime_camera.Net.WebServer;
import com.sohamsendev.realtime_camera.R;

import static android.content.Context.WIFI_SERVICE;

public class ControlPanelFragment extends Fragment {

    public static WebServer webServer;
    public static String ip;

    private TextView serverStatus;
    private EditText port, serverAddress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_control_panel, container, false);
    }

    @SuppressWarnings("ConstantConditions")
    // If the SuppressWarnings is not added, a wrong warning would be shown on some parts.
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WifiManager wm = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        serverStatus = (TextView) getView().findViewById(R.id.server_status);
        port = (EditText) getView().findViewById(R.id.port_number);
        // TODO: Add port security feature (max and min port number)

        serverAddress = (EditText) getView().findViewById(R.id.server_address);
        serverAddress.setText(R.string.server_address_not_running);

        FloatingActionButton fab = (FloatingActionButton) getView().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (webServer == null || !webServer.isRunning()) startWebServer();
                else stopWebServer();
                toggleStatus();
            }
        });
        super.onViewCreated(view, savedInstanceState);
    }

    private void toggleStatus() {
        if (webServer.isRunning()) {
            serverStatus.setText(R.string.server_running);
            serverStatus.setBackgroundResource(R.color.startServerBackgroundTint);
            serverStatus.setTextColor(getResources().getColor(R.color.startServerTint));
        } else {
            serverStatus.setText(R.string.server_not_running);
            serverStatus.setBackgroundResource(R.color.stopServerBackgroundTint);
            serverStatus.setTextColor(getResources().getColor(R.color.stopServerTint));
        }
    }

    private void startWebServer() {
        webServer = new WebServer(Integer.parseInt(port.getText().toString()),
                getActivity().getAssets());
        if (!webServer.isRunning()) {
            webServer.start();
            serverAddress.setText("http://" + ip + ":" + 8080);
        }
    }

    private void stopWebServer() {
        if (MainActivity.serverManager.isCameraServerRunning())
            MainActivity.serverManager.stopCameraServer();
        if (webServer != null) webServer.stop();
    }
}

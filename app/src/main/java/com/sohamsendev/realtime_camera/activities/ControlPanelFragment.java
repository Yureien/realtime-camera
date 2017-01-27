package com.sohamsendev.realtime_camera.activities;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sohamsendev.realtime_camera.R;
import com.sohamsendev.realtime_camera.media.CameraHolder;
import com.sohamsendev.realtime_camera.media.MediaBlock;
import com.sohamsendev.realtime_camera.media.OverlayView;
import com.sohamsendev.realtime_camera.servers.WebServer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.Context.WIFI_SERVICE;

public class ControlPanelFragment extends Fragment {

    public static WebServer webServer;
    private int MAX_PORT_NUMBER = 65535;
    private int MIN_PORT_NUMBER = 1024;
    public static String ip;

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

        WifiManager wm = (WifiManager) getActivity().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        final TextView serverStatus = (TextView) getView().findViewById(R.id.server_status);
        final EditText port = (EditText) getView().findViewById(R.id.port_number);
        //webServer = new WebServer(Integer.parseInt(String.valueOf(port.getText())));
        // TODO: Add port security feature (max and min port number)
        port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (webServer.isAlive()) {
                    webServer.stop();
                    webServer.closeAllConnections();
                }
                webServer = new WebServer(getContext(),
                        Integer.parseInt(editable.toString()));
                toggleStatus(serverStatus, webServer);
            }
        });

        final EditText serverAddress = (EditText) getView().findViewById(R.id.server_address);
        if (webServer != null && webServer.getHostname() != null)
            serverAddress.setText("http://" + ip + ":" + webServer.getListeningPort());
        else
            serverAddress.setText(R.string.server_address_not_running);

        FloatingActionButton fab = (FloatingActionButton) getView().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //TODO: Change button tint instead of toast
                    if (webServer == null) {
                        webServer = new WebServer(getContext(),
                                Integer.parseInt(port.getText().toString()));
                    }
                    if (!webServer.isAlive()) {
                        webServer.start();
                        serverAddress.setText("http://" + ip + ":" + webServer.getListeningPort());
                        if (!MainActivity.cameraPreview.isAdded())
                            getFragmentManager().beginTransaction()
                                    .add(R.id.fragment_container, MainActivity.cameraPreview)
                                    .hide(MainActivity.controlPanel)
                                    .show(MainActivity.cameraPreview)
                                    .commit();
                    } else {
                        webServer.stop();
                        serverAddress.setText(R.string.server_address_not_running);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                toggleStatus(serverStatus, webServer);
            }
        });
        super.onViewCreated(view, savedInstanceState);
    }

    private void toggleStatus(TextView statusTextView, WebServer webServer) {
        if (webServer.isAlive()) {
            statusTextView.setText(R.string.server_running);
            statusTextView.setBackgroundResource(R.color.startServerBackgroundTint);
            statusTextView.setTextColor(getResources().getColor(R.color.startServerTint));
        } else {
            statusTextView.setText(R.string.server_not_running);
            statusTextView.setBackgroundResource(R.color.stopServerBackgroundTint);
            statusTextView.setTextColor(getResources().getColor(R.color.stopServerTint));
        }
    }

    public static void closeServer() {
        if (webServer != null && webServer.isAlive()) {
            webServer.stop();
            webServer.closeAllConnections();
        }
    }
}

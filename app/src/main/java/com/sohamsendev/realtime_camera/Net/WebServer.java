package com.sohamsendev.realtime_camera.Net;

import android.content.res.AssetManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    private AssetManager assetManager;

    public WebServer(int port, AssetManager assetManager) {
        this.port = port;
        this.assetManager = assetManager;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        isRunning = true;
        new Thread(this).start();
    }

    public void stop() {
        if (isRunning) {
            try {
                isRunning = false;
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (isRunning) {
                Socket socket = serverSocket.accept();
                new WebSocket(socket, assetManager);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

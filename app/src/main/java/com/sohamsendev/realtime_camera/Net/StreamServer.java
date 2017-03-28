package com.sohamsendev.realtime_camera.Net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Stack;

public class StreamServer implements Runnable {

    static Stack<byte[]> bufferStack;
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning;

    public StreamServer(int port) {
        this.port = port;
        bufferStack = new Stack<>();
        bufferStack.setSize(100); // TODO: Fix this. No idea what to fix though.
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
            serverSocket.setPerformancePreferences(0, 2, 1);
            while (isRunning) {
                Socket socket = serverSocket.accept();
                new Thread(new StreamSocket(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBufferToStack(byte[] buffer) {
        bufferStack.push(buffer);
    }
}

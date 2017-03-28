package com.sohamsendev.realtime_camera.Net;

import android.util.Log;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Stack;

class StreamSocket implements Runnable {

    private Socket socket;
    private boolean isStreaming;

    private Stack<byte[]> bufferStack = StreamServer.bufferStack;

    StreamSocket(Socket socket) throws SocketException {
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(false);
        this.socket.setSendBufferSize(65536);
        isStreaming = true;
    }

    @Override
    public void run() {
        if (!isStreaming) return;

        PrintStream output;
        try {
            output = new PrintStream(socket.getOutputStream());

            // Initial header for M-JPEG.
            output.println("HTTP/1.1 200 OK");
            output.println("Server: Camera Android Server");
            output.println("Connection: close");
            output.println("Max-Age: 0");
            output.println("Expires: 0");
            output.println("Cache-Control: no-cache, private");
            output.println("Pragma: no-cache");
            output.println("Content-Type: multipart/x-mixed-replace;boundary=--boundary");
            output.println();

            // Start the loop for sending M-JPEGs
            while (isStreaming) {
                try {
                    if (bufferStack.empty()) continue;
                    byte[] buffer = bufferStack.peek();
                    if (buffer == null) continue;

                    output.println("--boundary");
                    output.println("Content-Type: image/jpeg");
                    output.println("Content-Length: " + buffer.length);
                    output.println();
                    output.write(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("StreamSocket", "IOException occurred");
            try {
                if (socket != null) socket.close();
                isStreaming = false;
                socket = null;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (socket != null) socket.close();
                isStreaming = false;
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}

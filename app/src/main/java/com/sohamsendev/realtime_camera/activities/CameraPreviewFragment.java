package com.sohamsendev.realtime_camera.activities;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.sohamsendev.realtime_camera.R;
import com.sohamsendev.realtime_camera.media.CameraHolder;
import com.sohamsendev.realtime_camera.media.MediaBlock;
import com.sohamsendev.realtime_camera.servers.WebServer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static com.sohamsendev.realtime_camera.activities.ControlPanelFragment.webServer;

public class CameraPreviewFragment extends Fragment implements CameraHolder.CameraReadyCallback {

    private StreamingServer streamingServer = null;
    private CameraHolder cameraHolder;
    Handler streamingHandler;

    private final int MediaBlockNumber = 3;
    private final int MediaBlockSize = 1024 * 512;
    private final int StreamingInterval = 100;

    ExecutorService executor = Executors.newFixedThreadPool(3);
    VideoEncodingTask videoTask = new VideoEncodingTask();
    private ReentrantLock previewLock = new ReentrantLock();
    boolean inProcessing = false;

    byte[] yuvFrame = new byte[1920 * 1280 * 2];
    MediaBlock[] mediaBlocks = new MediaBlock[MediaBlockNumber];
    int mediaWriteIndex = 0;
    int mediaReadIndex = 0;

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
        cameraHolder = new CameraHolder(getContext());
        cameraContainer.addView(cameraHolder);

        // Initialize streamer
        for (int i = 0; i < MediaBlockNumber; i++) {
            mediaBlocks[i] = new MediaBlock(MediaBlockSize);
        }
        resetMediaBuffer();

        try {
            int streamingPort = 8088;
            streamingServer = new StreamingServer(streamingPort);
            streamingServer.start();
        } catch (IOException e) {
            return;
        }

        if (webServer.isAlive()) {
            webServer.registerCGI("/cgi/query", doQuery);
            cameraHolder.setCameraReadyCallback(this);
        }

        streamingHandler = new Handler();
        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, StreamingInterval);
    }

    private WebServer.CommonGatewayInterface doQuery = new WebServer.CommonGatewayInterface () {
        @Override
        public String run(Map<String, String> parms) {
            String ret;
            if (streamingServer.inStreaming) {
                ret = "{\"state\": \"busy\"}";
            } else {
                ret = "{\"state\": \"ok\",";
                ret = ret + "\"width\": \"" + cameraHolder.previewWidth() + "\",";
                ret = ret + "\"height\": \"" + cameraHolder.previewHeight() + "\"}";
            }
            return ret;
        }

        @Override
        public InputStream streaming(Map<String, String> parms) {
            return null;
        }
    };

    private void doStreaming() {
        synchronized (CameraPreviewFragment.this) {
            MediaBlock targetBlock = mediaBlocks[mediaReadIndex];
            if (targetBlock.flag == 1) {
                streamingServer.sendMedia(targetBlock.data(), targetBlock.length());
                targetBlock.reset();

                mediaReadIndex++;
                if (mediaReadIndex >= MediaBlockNumber)
                    mediaReadIndex = 0;
            }
        }
        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, StreamingInterval);
    }

    @Override
    public void onCameraReady() {
        Toast.makeText(getContext(), "Camera Ready", Toast.LENGTH_SHORT).show();
        cameraHolder.stopPreview();
        int pictureHeight = 360;
        int pictureWidth = 480;
        cameraHolder.setupCamera(pictureWidth, pictureHeight, 4, 25.0, previewCallback);

        nativeInitMediaEncoder(cameraHolder.previewWidth(), cameraHolder.getHeight());
        cameraHolder.startPreview();
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] frame, Camera c) {
            previewLock.lock();
            doVideoEncode(frame);
            c.addCallbackBuffer(frame);
            previewLock.unlock();
        }
    };

    private void doVideoEncode(byte[] frame) {
        if (!inProcessing) {
            inProcessing = true;
            int picWidth = cameraHolder.previewWidth();
            int picHeight = cameraHolder.previewHeight();

            int size = picWidth * picHeight + picWidth * picHeight / 2;
            System.arraycopy(frame, 0, yuvFrame, 0, size);

            executor.execute(videoTask);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraHolder != null) {
            previewLock.lock();
            cameraHolder.stopPreview();
            cameraHolder.release();
            previewLock.unlock();
            cameraHolder = null;
        }
    }

    private class VideoEncodingTask implements Runnable {
        private byte[] resultNal = new byte[1024 * 1024];
        private byte[] videoHeader = new byte[8];

        private VideoEncodingTask() {
            videoHeader[0] = (byte) 0x19;
            videoHeader[1] = (byte) 0x79;
        }

        public void run() {
            MediaBlock currentBlock = mediaBlocks[mediaWriteIndex];
            if (currentBlock.flag == 1) {
                inProcessing = false;
                return;
            }
            int intraFlag = 0;
            if (currentBlock.videoCount == 0) {
                intraFlag = 1;
            }
            int millis = (int) (System.currentTimeMillis() % 65535);
            int ret = nativeDoVideoEncode(yuvFrame, resultNal, intraFlag);
            if (ret <= 0) {
                return;
            }
            // timestamp
            videoHeader[2] = (byte) (millis & 0xFF);
            videoHeader[3] = (byte) ((millis >> 8) & 0xFF);
            // length
            videoHeader[4] = (byte) (ret & 0xFF);
            videoHeader[5] = (byte) ((ret >> 8) & 0xFF);
            videoHeader[6] = (byte) ((ret >> 16) & 0xFF);
            videoHeader[7] = (byte) ((ret >> 24) & 0xFF);

            synchronized (CameraPreviewFragment.this) {
                if (currentBlock.flag == 0) {
                    boolean changeBlock = false;

                    if (currentBlock.length() + ret + 8 <= MediaBlockSize) {
                        currentBlock.write(videoHeader, 8);
                        currentBlock.writeVideo(resultNal, ret);
                    } else changeBlock = true;

                    int estimatedFrameNumber = 30;
                    if (!changeBlock)
                        if (currentBlock.videoCount >= estimatedFrameNumber) changeBlock = true;

                    if (changeBlock) {
                        currentBlock.flag = 1;

                        mediaWriteIndex++;
                        if (mediaWriteIndex >= MediaBlockNumber) mediaWriteIndex = 0;
                    }
                }
            }
            inProcessing = false;
        }
    }

    private class StreamingServer extends WebSocketServer {

        WebSocket mediaSocket = null;
        boolean inStreaming = false;
        ByteBuffer buf = ByteBuffer.allocate(MediaBlockSize);

        StreamingServer(int port) throws UnknownHostException {
            super(new InetSocketAddress(port));
        }

        boolean sendMedia(byte[] data, int length) {
            boolean ret = false;
            if (inStreaming) {
                buf.clear();
                buf.put(data, 0, length);
                buf.flip();
                mediaSocket.send(buf);
                ret = true;
            }
            return ret;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            if (inStreaming) {
                conn.close();
            } else {
                resetMediaBuffer();
                mediaSocket = conn;
                inStreaming = true;
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            if (conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            if (conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }
    }

    private void resetMediaBuffer() {
        synchronized (CameraPreviewFragment.this) {
            for (int i = 1; i < MediaBlockNumber; i++) {
                mediaBlocks[i].reset();
            }
            mediaWriteIndex = 0;
            mediaReadIndex = 0;
        }
    }

    private native void nativeInitMediaEncoder(int width, int height);

    //private native void nativeReleaseMediaEncoder(int width, int height);

    private native int nativeDoVideoEncode(byte[] in, byte[] out, int flag);

    static {
        System.loadLibrary("video-encoder");
    }
}

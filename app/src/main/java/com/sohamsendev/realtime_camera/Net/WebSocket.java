package com.sohamsendev.realtime_camera.Net;

import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import com.sohamsendev.realtime_camera.Activities.MainActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle the incoming socket connection in separate threads.
 */

class WebSocket implements Runnable {

    private Socket socket;
    private boolean isRunning;
    private AssetManager assets;
    private HashMap<String, String> htmlMap;

    WebSocket(Socket socket, AssetManager assetManager) {
        this.socket = socket;
        this.assets = assetManager;
        isRunning = true;
        htmlMap = new HashMap<>();
        htmlMap.put("playback-url", "http://" + socket.getLocalAddress() + ":" +
                String.valueOf((socket.getLocalPort() + 1))); // Stream Server port: Web Server port + 1
        new Thread(this).start();
    }

    @Override
    public void run() {
        if (!isRunning) return;
        PrintStream output = null;
        BufferedReader reader = null;
        try {
            String url = "";
            // Read out the incoming HTTP request to a map.
            Map<String, String> requestMap = new HashMap<>();
            Map<String, String> dataMap = new HashMap<>();
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = reader.readLine();
            boolean isPost = line.startsWith("POST");

            int contentLength = 0;
            while (!line.equals("")) {
                if (isPost) {
                    final String contentHeader = "Content-Length: ";
                    if (line.startsWith(contentHeader)) {
                        contentLength = Integer.parseInt(line.substring(contentHeader.length()));
                    }
                } else {
                    if (line.startsWith("GET /")) {
                        int start = line.indexOf('/') + 1;
                        int end = line.indexOf(' ', start);
                        url = line.substring(start, end);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.indexOf(":") <= 0) {
                        line = reader.readLine();
                        continue;
                    }

                    line = line.replace("\r\n", "");
                    String key = line.substring(line.indexOf(":"));
                    String value = line.substring(line.indexOf(":") + 2, line.length());

                    requestMap.put(key, value);
                }
                line = reader.readLine();
            }
            if (isPost) {
                char[] postBuffer = new char[contentLength];
                //noinspection unused
                int c = reader.read(postBuffer);
                for (String l : new String(postBuffer).split("&")) {
                    String[] kv = l.split("=");
                    // key is 0, value is 1
                    if (kv[1].contains("+")) kv[1] = kv[1].replace("+", " ");
                    dataMap.put(kv[0], kv[1]);
                }
            }

            for (String key : dataMap.keySet()) {
                Log.i("WebSocket", "KEY: " + key + " VALUE: " + dataMap.get(key));
            }

            processPOSTData(dataMap);

            output = new PrintStream(socket.getOutputStream());

            switch (url) {
                case "test":
                    String testMessage = "Hello World!";
                    output.println("HTTP /1.1 200 OK");
                    output.println("Content-Type: text/plain");
                    output.println("Content-Length: " + testMessage.length());
                    output.println();
                    output.println(testMessage);
                    output.flush();
                    break;
                case "":
                    byte[] bytes = loadContent("index.html");
                    if (null == bytes) {
                        writeServerError(output);
                        return;
                    }
                    // Send out the content.
                    output.println("HTTP/1.0 200 OK");
                    output.println("Content-Type: " + detectMimeType(".html"));
                    output.println("Content-Length: " + bytes.length);
                    output.println();
                    output.write(bytes);
                    output.flush();
                    break;
                default:
                    bytes = loadContent(url);
                    if (null == bytes) {
                        writeServerError(output);
                        return;
                    }
                    // Send out the content.
                    output.println("HTTP/1.0 200 OK");
                    output.println("Content-Type: " + detectMimeType(url));
                    output.println("Content-Length: " + bytes.length);
                    output.println();
                    output.write(bytes);
                    output.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) output.close();
                if (reader != null) reader.close();
                if (!socket.isClosed() || socket.isConnected()) socket.close();
                isRunning = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Process the incoming POST data
     *
     * @param dataMap POST data inserted inside a map
     */
    private void processPOSTData(Map<String, String> dataMap) {
        for (String key : dataMap.keySet()) {
            switch (key) {
                case "start":
                    if (dataMap.get(key).equals("Start"))
                        MainActivity.serverManager.startCameraServer();
                    if (dataMap.get(key).equals("Stop"))
                        MainActivity.serverManager.stopCameraServer();
            }
        }
    }

    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }

    /**
     * Loads all the content of {@code fileName}.
     * To use dynamic files, specify a '_' sign before the file in assets and add the content
     * to be replaced in htmlMap. This code will automatically handle the rest.
     *
     * @param fileName The name of the file.
     * @return The content of the file.
     * @throws IOException if some error occurs.
     */
    private byte[] loadContent(String fileName) throws IOException {
        boolean toModify = false;
        for (String file : assets.list("net")) {
            if (file.startsWith("_")) {
                if (fileName.equals(file.substring(1))) fileName = file;
                toModify = true;
                break;
            }
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (toModify) {
                BufferedReader input = new BufferedReader(new InputStreamReader(assets.open("net/" + fileName)));
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.contains("<!") && line.contains("!>")) {
                        String id = line.substring(line.indexOf("<!") + 2, line.indexOf("!>"));
                        line = line.replace("<!" + id + "!>", htmlMap.get(id));
                    }
                    output.write(line.getBytes());
                }
                input.close();
            } else { // If not modifying the files, use a faster method.
                InputStream input = assets.open(fileName);
                byte[] buffer = new byte[2048];
                int size;
                while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
                input.close();
            }
            output.flush();
            return output.toByteArray();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * Detects the MIME type from the {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return A MIME type.
     */
    private String detectMimeType(String fileName) {
        if (TextUtils.isEmpty(fileName)) return "text/plain";
        else if (fileName.endsWith(".html")) return "text/html";
        else if (fileName.endsWith(".js")) return "application/javascript";
        else if (fileName.endsWith(".css")) return "text/css";
        else return "application/octet-stream";
    }
}
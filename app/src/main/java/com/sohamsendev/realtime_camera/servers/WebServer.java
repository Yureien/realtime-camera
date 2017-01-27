package com.sohamsendev.realtime_camera.servers;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

    private Context context;

    public static final String
            MIME_PLAINTEXT = "text/plain",
            MIME_HTML = "text/html",
            MIME_JS = "application/javascript",
            MIME_CSS = "text/css",
            MIME_PNG = "image/png",
            MIME_DEFAULT_BINARY = "application/octet-stream",
            MIME_XML = "text/xml";

    public WebServer(Context context, int port) {
        super(port);
        this.context = context;

    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getUri().startsWith("/cgi/"))
            return serveCGI(session);
        else if (session.getUri().startsWith("/stream/"))
            return serveStream(session);
        else return serveStream(session);
    }

    private Response serveStream(IHTTPSession session) {
        CommonGatewayInterface cgi = cgiEntries.get(session.getUri());
        InputStream mbuffer;
        try {
            if (session.getUri() != null) {
                if (session.getUri().contains(".js")) {
                    mbuffer = context.getAssets().open(session.getUri().substring(1));
                    return newChunkedResponse(Response.Status.OK, MIME_JS, mbuffer);
                } else if (session.getUri().contains(".css")) {
                    mbuffer = context.getAssets().open(session.getUri().substring(1));
                    return newChunkedResponse(Response.Status.OK, MIME_CSS, mbuffer);

                } else if (session.getUri().contains(".png")) {
                    mbuffer = context.getAssets().open(session.getUri().substring(1));
                    return newChunkedResponse(Response.Status.OK, MIME_PNG, mbuffer);
                } else {
                    mbuffer = context.getAssets().open("index.html");
                    return newChunkedResponse(Response.Status.OK, MIME_HTML, mbuffer);
                }
            }
        } catch (IOException e) {
            Log.d("Web Server", "Error opening file" + session.getUri().substring(1));
            e.printStackTrace();
        }
        return null;
    }

    private Response serveCGI(IHTTPSession session) {
        CommonGatewayInterface cgi = cgiEntries.get(session.getUri());
        if (cgi == null)
            return null;
        String msg = cgi.run(session.getParms());
        if (msg == null)
            return null;
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, msg);
    }

    public static interface CommonGatewayInterface {
        public String run(Map<String, String> parms);

        public InputStream streaming(Map<String, String> parms);
    }

    private HashMap<String, CommonGatewayInterface> cgiEntries = new HashMap<String, CommonGatewayInterface>();

    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if (cgi != null)
            cgiEntries.put(uri, cgi);
    }
}

package com.hackernate.trivia;

import android.app.Application;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class TriviaApplication extends Application {
    private Socket socket;
    private static final String URL = "http://10.0.2.2:3000";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            socket = IO.socket(URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return socket;
    }
}

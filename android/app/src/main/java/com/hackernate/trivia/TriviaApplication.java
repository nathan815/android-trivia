package com.hackernate.trivia;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class TriviaApplication extends Application {
    private Socket socket;
    private SharedPreferences sharedPrefs;

    private static final String URL = "http://10.0.2.2:3000";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            socket = IO.socket(URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        sharedPrefs = getSharedPreferences("application", Context.MODE_PRIVATE);
    }

    public Socket getSocket() {
        return socket;
    }

    public SharedPreferences getSharedPrefs() {
        return sharedPrefs;
    }
}

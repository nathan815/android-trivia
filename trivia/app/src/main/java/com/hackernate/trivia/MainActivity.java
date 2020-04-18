package com.hackernate.trivia;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import io.socket.client.Socket;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        TriviaApplication app = (TriviaApplication) getApplication();
        Socket socket = app.getSocket();

        socket.on(Socket.EVENT_CONNECT, (args) -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "connected!", Toast.LENGTH_LONG).show();
            });
        });
        socket.connect();
    }
}

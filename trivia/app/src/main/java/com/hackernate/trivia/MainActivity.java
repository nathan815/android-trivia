package com.hackernate.trivia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.socket.client.Socket;


public class MainActivity extends AppCompatActivity {

    String username;
    Socket socket;

    TextView usernameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        usernameText = findViewById(R.id.username);

        TriviaApplication app = (TriviaApplication) getApplication();
        socket = app.getSocket();
        socket.connect();

        socket.on(Socket.EVENT_CONNECT, (args) -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "connected!", Toast.LENGTH_LONG).show();
            });
        });

        askForUsername();
    }

    private void setUpInterface(String username) {
        this.username = username;
        usernameText.setText("Welcome, " + username);
    }

    private void askForUsername() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String username = sharedPref.getString("username", "");

        if (!"".equals(username)) {
            setUpInterface(username);
            return;
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Hello! Enter a username")
                .setView(input)
                .setPositiveButton("Continue", null)
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = input.getText().toString();
                sharedPref.edit().putString("username", name).apply();
                setUpInterface(name);
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}

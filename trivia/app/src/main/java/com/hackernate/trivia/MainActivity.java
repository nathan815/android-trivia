package com.hackernate.trivia;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hackernate.trivia.data.User;

import java.util.UUID;
import java.util.function.BiFunction;

import io.socket.client.Socket;


public class MainActivity extends AppCompatActivity {

    Socket socket;
    MainManager manager;

    TextView usernameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        usernameText = findViewById(R.id.username);

        TriviaApplication app = (TriviaApplication) getApplication();
        socket = app.getSocket();
        socket.connect();

        manager = MainManager.getInstance(app);

        socket.on(Socket.EVENT_CONNECT, (args) -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
            });
            socket.emit("syn");
        });

        askForUsername();
    }

    private void setUpInterface(User user) {
        manager.userEntered(user);
        usernameText.setText("Welcome, " + user.username);
    }

    private void askForUsername() {
        User savedUser = manager.getSavedUser();
        if (savedUser != null) {
            setUpInterface(savedUser);
            return;
        }

        inputDialog("Hello! Enter your name", "Continue", null, (name, dialog) -> {
            if("".equals(name)) {
                return false;
            }
            User user = new User(UUID.randomUUID().toString(), name);
            manager.setSavedUser(user);
            setUpInterface(user);
            return true;
        });
    }

    public void newGame(View v) {
        inputDialog("What would you like to call this game?", "Create", "Cancel", (name, dialog) -> {
            manager.createGame(name, (id) -> {
                runOnUiThread(dialog::dismiss);
                navigateToGame(id);
                dialog.dismiss();
            });
            return false;
        });
    }

    public void joinGame(View v) {
        inputDialog("Please enter the game code", "Join", "Cancel", (id, dialog) -> {
            manager.joinGame(id, (success, error) -> {
                if(success) {
                    runOnUiThread(dialog::dismiss);
                    navigateToGame(id);
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return false;
        });
    }

    private void navigateToGame(String id) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    private void inputDialog(String title, String positiveButtonText, String negativeButtonText,
                             BiFunction<String, Dialog, Boolean> callback) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextSize(25);
        input.setPadding(25, 40, 25, 40);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positiveButtonText, null)
                .setNegativeButton(negativeButtonText, null)
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = input.getText().toString().trim();
                if(callback.apply(name, dialog)) {
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
    }
}

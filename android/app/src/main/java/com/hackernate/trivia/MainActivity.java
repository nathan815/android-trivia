package com.hackernate.trivia;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.User;

import java.util.Collections;
import java.util.UUID;

import io.socket.client.Socket;


public class MainActivity extends AppCompatActivity {

    Socket socket;
    MainManager manager;

    TextView usernameText, gameListNoGamesText;
    LinearLayout gameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameText = findViewById(R.id.username);
        gameList = findViewById(R.id.game_list);
        gameListNoGamesText = findViewById(R.id.game_list_message);

        TriviaApplication app = (TriviaApplication) getApplication();
        socket = app.getSocket();
        socket.connect();

        manager = MainManager.getInstance(app);

        socket.on(Socket.EVENT_CONNECT, (args) -> {
            runOnUiThread(() -> {
                Utils.showBottomToast(this, "Connected", Toast.LENGTH_LONG);
            });
        });

        askForUsername();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderGameList();
    }

    private void setUpInterface(User user) {
        manager.userEntered(user);
        usernameText.setText("Welcome, " + user.username);
    }

    private void renderGameList() {
        manager.getMyGames(games -> runOnUiThread(() -> {
            gameList.removeAllViews();
            gameListNoGamesText.setVisibility(games.size() == 0 ? View.VISIBLE : View.GONE);
            Collections.reverse(games);
            for(Game game : games) {
                gameList.addView(makeGameListRow(game));
            }
        }));
    }

    private View makeGameListRow(Game game) {
        View row = getLayoutInflater().inflate(R.layout.game_list_row, gameList, false);
        TextView nameText = row.findViewById(R.id.game_row_name);
        nameText.setText(game.name);
        TextView statusText = row.findViewById(R.id.game_row_status_text);
        statusText.setText(game.getFormattedStatusText() + " - " + game.id);
        Button viewBtn = row.findViewById(R.id.game_row_btn);
        viewBtn.setOnClickListener((view) -> {
            navigateToGame(game.id);
            view.setEnabled(false);
        });
        return row;
    }

    private void askForUsername() {
        User savedUser = manager.getSavedUser();
        if (savedUser != null) {
            setUpInterface(savedUser);
            return;
        }

        Utils.showInputDialog(this, "Hello! Enter your name", "Continue", null, (name, dialog) -> {
            if ("".equals(name)) {
                return false;
            }
            User user = new User(UUID.randomUUID().toString(), name);
            manager.setSavedUser(user);
            setUpInterface(user);
            return true;
        });
    }

    public void newGame(View v) {
        Utils.showInputDialog(this, "What would you like to call this game?", "Create", "Cancel", (name, dialog) -> {
            manager.createGame(name, (id) -> runOnUiThread(() -> {
                dialog.dismiss();
                navigateToGame(id);
            }));
            return false;
        });
    }

    public void joinGame(View v) {
        Utils.showInputDialog(this, "Please enter the game code", "Join", "Cancel", (id, dialog) -> {
            manager.joinGame(id, (success, error) -> runOnUiThread(() -> {
                if (success) {
                    dialog.dismiss();
                    navigateToGame(id);
                } else {
                    Utils.showTopToast(this, "Error: " + error, Toast.LENGTH_LONG);
                }
            }));
            return false;
        });
    }

    private void navigateToGame(String id) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }

}

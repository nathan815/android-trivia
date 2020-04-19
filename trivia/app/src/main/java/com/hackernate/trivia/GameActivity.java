package com.hackernate.trivia;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.User;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import io.socket.client.Socket;

public class GameActivity extends AppCompatActivity implements GameManager.Listener {

    private static final String TAG = GameActivity.class.getName();

    private Socket socket;
    private String gameId;
    private GameManager gameManager;
    private Game game;

    View gameNotStartedContainer, gameStartedContainer;
    TextView gameCodeText, playersInGameCountText, playersInGameNamesText;
    Button startGameBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_game);

        gameNotStartedContainer = findViewById(R.id.game_not_started);
        gameCodeText = findViewById(R.id.game_code_text);
        playersInGameCountText = findViewById(R.id.players_in_game_count_text);
        playersInGameNamesText = findViewById(R.id.players_in_game_names_text);
        startGameBtn = findViewById(R.id.start_game_btn);

        gameStartedContainer = findViewById(R.id.game_started);

        socket = ((TriviaApplication) getApplication()).getSocket();

        Intent intent = getIntent();
        gameId = intent.getStringExtra("id");

        gameManager = new GameManager(gameId, socket, this);

        getSupportActionBar().setTitle("Loading...");
    }

    @Override
    public void gameLoaded() {
        runOnUiThread(() -> {
            game = gameManager.getGame();
            drawUi();
        });
    }

    private void drawUi() {
        if (game == null) {
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(game.name);
        }
        if (game.isWaitingToStart()) {

            gameStartedContainer.setVisibility(View.GONE);
            gameNotStartedContainer.setVisibility(View.VISIBLE);
            if (game.canBeStarted()) {
                startGameBtn.setEnabled(true);
                startGameBtn.setAlpha(1.0f);
            }
            playersInGameCountText.setText("Players in Game: " + game.playersCount() + "/4");
            playersInGameNamesText.setText(game.getPlayers().stream()
                    .map(u -> u.username)
                    .collect(Collectors.joining(", ")));
            gameCodeText.setText(game.id);

        } else if (game.hasStarted()) {
            gameStartedContainer.setVisibility(View.VISIBLE);
            gameNotStartedContainer.setVisibility(View.GONE);
        }
    }

    public void copyCode(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("game code", game.id);
        clipboard.setPrimaryClip(clip);
        Utils.showBottomToast(this, "Copied to clipboard", Toast.LENGTH_SHORT);
    }
}



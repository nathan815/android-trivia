package com.hackernate.trivia;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.hackernate.trivia.data.Game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;

public class GameActivity extends AppCompatActivity implements GameManager.Listener {

    private static final String TAG = GameActivity.class.getName();

    private Socket socket;
    private String gameId;
    private GameManager gameManager;
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_game);

        socket = ((TriviaApplication)getApplication()).getSocket();

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
        if(getSupportActionBar() != null && game != null) {
            getSupportActionBar().setTitle(game.name);
        }
        if(game.hasStarted()) {

        } else {

        }
    }
}

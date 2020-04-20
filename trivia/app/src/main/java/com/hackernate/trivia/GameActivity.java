package com.hackernate.trivia;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.socket.client.Socket;

public class GameActivity extends AppCompatActivity implements GameManager.Listener {

    private static final String TAG = GameActivity.class.getName();

    private Socket socket;
    private String gameId;
    private MainManager mainManager;
    private GameManager gameManager;
    private Game game;

    View gameNotStartedContainer, gameStartedContainer;
    TextView gameCodeText, playersInGameCountText, playersInGameNamesText,
            waitingOnOwnerStartGameText, questionText, questionPointsText;
    RadioGroup answerRadioGroup;
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
        waitingOnOwnerStartGameText = findViewById(R.id.waiting_owner_start_game);

        gameStartedContainer = findViewById(R.id.game_started);
        questionText = findViewById(R.id.game_question_text);
        questionPointsText = findViewById(R.id.game_question_points);
        answerRadioGroup = findViewById(R.id.game_answers_radio_group);

        TriviaApplication app = (TriviaApplication) getApplication();
        socket = app.getSocket();

        Intent intent = getIntent();
        gameId = intent.getStringExtra("id");

        mainManager = MainManager.getInstance(app);
        gameManager = new GameManager(gameId, socket, this);

        getSupportActionBar().setTitle("Loading...");

        gameStartedContainer.setVisibility(View.GONE);
        gameNotStartedContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameManager.stopListeners();
    }

    @Override
    public void gameLoaded() {
        runOnUiThread(() -> {
            game = gameManager.getGame();
            drawUi();
        });
    }

    @Override
    public void gameUpdated() {
        runOnUiThread(this::drawUi);
    }

    private void drawUi() {
        if (game == null) {
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(game.name);
        }
        if (game.isWaitingToStart()) {
            showWaiting();
        } else if (game.hasStarted()) {
            showGamePlay();
        } else {
            // game completed
        }
    }

    private void showWaiting() {
        gameNotStartedContainer.setVisibility(View.VISIBLE);
        boolean isOwner = game.getOwnerId().equals(mainManager.getSavedUser().id);

        waitingOnOwnerStartGameText.setVisibility(View.GONE);

        if(game.canBeStarted()) {
            if (isOwner) {
                startGameBtn.setEnabled(true);
                startGameBtn.setAlpha(1.0f);
            } else {
                waitingOnOwnerStartGameText.setVisibility(View.VISIBLE);
                waitingOnOwnerStartGameText.setText("Waiting for " + game.getOwner().username + " to start");
            }
        }

        playersInGameCountText.setText("Players in Game: " + game.playersCount() + "/4");
        playersInGameNamesText.setText(game.getPlayers().stream()
                .map(u -> u.username)
                .collect(Collectors.joining(", ")));
        gameCodeText.setText(game.id);
    }

    private void showGamePlay() {
        gameStartedContainer.setVisibility(View.VISIBLE);
        Question q = game.getLatestQuestion();
        if(q == null) {
            Utils.showBottomToast(this, "No question to show", Toast.LENGTH_LONG);
        } else {
            displayQuestion(q);
        }
    }

    private void displayQuestion(Question question) {
        questionText.setText(question.getQuestion());
        questionPointsText.setText(question.getPoints() + " points");
        answerRadioGroup.removeAllViews();
        List<RadioButton> radios = new ArrayList<>();
        int i = 0;
        for(String answer : question.getAnswers()) {
            RadioButton radio = new RadioButton(this);
            radio.setText(answer);
            radio.setTextSize(25);
            radio.setTag(i);
            radios.add(radio);
            i++;
        }
        Collections.shuffle(radios);
        for(RadioButton radio : radios) {
            answerRadioGroup.addView(radio);
        }
    }

    public void startGame(View v) {
        gameManager.startGame();
    }

    public void copyCode(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("game code", game.id);
        clipboard.setPrimaryClip(clip);
        Utils.showBottomToast(this, "Copied to clipboard", Toast.LENGTH_SHORT);
    }

    public void submitAnswer(View v) {
        int id = answerRadioGroup.getCheckedRadioButtonId();
        if(id == -1) {
            return;
        }
        RadioButton btn = findViewById(id);
        gameManager.submitAnswerForCurrentQuestion((int)btn.getTag());
    }
}



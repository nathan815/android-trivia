package com.hackernate.trivia;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.Question;
import com.hackernate.trivia.data.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import io.socket.client.Socket;

public class GameActivity extends AppCompatActivity implements GameManager.Listener {

    private final static int NEXT_QUESTION_COUNTDOWN_MS = 3100;

    private static final String TAG = GameActivity.class.getName();

    private Socket socket;
    private String gameId;
    private MainManager mainManager;
    private GameManager gameManager;
    private Game game;
    private User currentUser;

    View gameNotStartedContainer, gameStartedContainer;
    LinearLayout gameScoresContainer;
    TextView gameCodeText, playersInGameCountText, playersInGameNamesText,
            waitingOnOwnerStartGameText, questionText, questionCounterText, questionPointsText,
            messageUnderSubmitButtonText;
    RadioGroup answerRadioGroup;
    Button startGameBtn, submitBtn;

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
        questionCounterText = findViewById(R.id.game_question_counter);
        questionPointsText = findViewById(R.id.game_question_points);
        answerRadioGroup = findViewById(R.id.game_answers_radio_group);
        submitBtn = findViewById(R.id.submit_answer);
        messageUnderSubmitButtonText = findViewById(R.id.message_under_submit_button);
        gameScoresContainer = findViewById(R.id.game_scores);

        TriviaApplication app = (TriviaApplication) getApplication();
        socket = app.getSocket();

        Intent intent = getIntent();
        gameId = intent.getStringExtra("id");

        mainManager = MainManager.getInstance(app);
        currentUser = mainManager.getSavedUser();
        gameManager = new GameManager(gameId, currentUser, socket, this);

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
            renderUi();
        });
    }

    @Override
    public void gameUpdated() {
        runOnUiThread(this::renderUi);
    }

    @Override
    public void updatedWithPlayerScores(String userId) {
        runOnUiThread(() -> {
            // If this is an update of scores for the user on this device,
            // also re-render answers/submit button
            if (currentUser.getId().equals(userId)) {
                disableAnswers();
            }
            renderPlayerScoresBox();
        });
    }

    @Override
    public void updatedWithNextQuestion() {
        System.out.println("updatedWithNextQuestion");
        runOnUiThread(() -> {
            // If we are rendering the first question skip the countdown
            if (game.questions.size() == 1) {
                System.out.println("updatedWithNextQuestion render first question");
                System.out.println(game);
                renderUi();
                return;
            }
            // start next question countdown
            new CountDownTimer(NEXT_QUESTION_COUNTDOWN_MS, 1000) {
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    messageUnderSubmitButtonText.setVisibility(View.VISIBLE);
                    messageUnderSubmitButtonText.setText("Next question in " + seconds + "...");
                }

                public void onFinish() {
                    messageUnderSubmitButtonText.setVisibility(View.GONE);
                    messageUnderSubmitButtonText.setText("");
                    renderUi();
                }
            }.start();
        });
    }

    private void renderUi() {
        if (game == null) {
            System.out.println("renderUi: game is null");
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Trivia Friends: " + game.name);
        }

        // reset: hide everything at first
        gameStartedContainer.setVisibility(View.GONE);
        gameNotStartedContainer.setVisibility(View.GONE);

        if (game.isWaitingToStart()) {
            renderWaitingView();
        } else if (game.hasStarted() || game.isDone()) {
            messageUnderSubmitButtonText.setVisibility(View.GONE);
            renderGamePlay();
            if (game.isDone()) {
                renderGameOverIndicator();
            }
        }
    }

    private void renderWaitingView() {
        gameNotStartedContainer.setVisibility(View.VISIBLE);
        boolean isOwner = game.getOwnerId().equals(currentUser.getId());

        waitingOnOwnerStartGameText.setVisibility(View.GONE);

        if (game.canBeStarted()) {
            if (isOwner) {
                startGameBtn.setEnabled(true);
                startGameBtn.setAlpha(1.0f);
            } else {
                waitingOnOwnerStartGameText.setVisibility(View.VISIBLE);
                waitingOnOwnerStartGameText.setText("Waiting for " + game.getOwner().getUsername() + " to start");
            }
        }

        playersInGameCountText.setText("Players in Game: " + game.playersCount() + "/4");
        playersInGameNamesText.setText(game.getPlayers().stream()
                .map(User::getUsername)
                .collect(Collectors.joining(", ")));
        gameCodeText.setText(game.id);
    }

    private void renderGamePlay() {
        Question q = game.getLatestQuestion();
        System.out.println("renderGamePlayer, q="+q);
        if (q == null) {
            gameStartedContainer.setVisibility(View.GONE);
            Utils.showBottomToast(this, "No question to show", Toast.LENGTH_LONG);
        } else {
            gameStartedContainer.setVisibility(View.VISIBLE);
            renderQuestion(q);
            renderPlayerScoresBox();
        }
    }

    private void renderQuestion(Question question) {
        System.out.println("renderQuestion: "+question);
        questionText.setText(Html.fromHtml(question.getQuestion(), 0));
        questionCounterText.setText(game.askedQuestionsCount() + "/" + game.maxQuestionCount());
        questionPointsText.setText(question.getPoints() + " points");
        answerRadioGroup.removeAllViews();
        List<RadioButton> radios = new ArrayList<>();
        int i = 0;
        int selectedIndex = game.getCurrentSelectedAnswer(currentUser.getId());
        boolean hasSelectedAnswer = selectedIndex != -1;

        for (String answer : question.getAnswers()) {
            RadioButton radio = new RadioButton(this);
            radio.setText(Html.fromHtml(answer, 0));
            radio.setTextSize(25);
            radio.setTag(i);
            radio.setChecked(selectedIndex == i);
            radio.setEnabled(!hasSelectedAnswer);
            radios.add(radio);
            i++;
        }

        // create a random object seeded from the game ID and current user ID
        // for deterministic shuffling per game and user
        Random random = new Random((gameId + currentUser.getId()).hashCode());
        Collections.shuffle(radios, random);

        for (RadioButton radio : radios) {
            answerRadioGroup.addView(radio);
        }

        if (hasSelectedAnswer) {
            submitBtn.setEnabled(false);
            submitBtn.setAlpha(0.4f);
        } else {
            submitBtn.setEnabled(true);
            submitBtn.setAlpha(1.0f);
        }
    }

    private void disableAnswers() {
        submitBtn.setEnabled(false);
        submitBtn.setAlpha(0.4f);
        for (int i = 0; i < answerRadioGroup.getChildCount(); i++) {
            answerRadioGroup.getChildAt(i).setEnabled(false);
        }
    }

    private void renderPlayerScoresBox() {
        gameScoresContainer.removeAllViews();
        Question question = game.getLatestQuestion();
        if (question == null) return;
        for (Map.Entry<User, Integer> entry : game.getPlayerPointsMap().entrySet()) {
            User user = entry.getKey();
            int points = entry.getValue();

            TextView tv = new TextView(this);

            int currentAnswerIndex = game.getCurrentSelectedAnswer(user.getId());
            boolean isUserCorrect = currentAnswerIndex == question.getCorrectIndex();
            String currentAnswerInfo = currentAnswerIndex != -1 ? (isUserCorrect ? "✔︎" : "⛔️") : "";
            String suffix = (points == 1 ? "" : "s");

            tv.setText(String.format("%s: %d point%s %s", user.getUsername(), points, suffix, currentAnswerInfo));
            tv.setTextSize(20);

            if (user.equals(currentUser)) {
                tv.setTypeface(null, Typeface.BOLD);
            }

            gameScoresContainer.addView(tv);
        }
    }

    private void renderGameOverIndicator() {
        messageUnderSubmitButtonText.setText("Game Over");
        messageUnderSubmitButtonText.setVisibility(View.VISIBLE);
    }

    public void startGame(View v) {
        gameManager.startGame();
    }

    public void copyCode(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("game code", game.id);
        clipboard.setPrimaryClip(clip);
        Utils.showBottomToast(this, "Copied to cli️pboard", Toast.LENGTH_SHORT);
    }

    public void submitAnswer(View v) {
        int id = answerRadioGroup.getCheckedRadioButtonId();
        if (id == -1) {
            return;
        }
        RadioButton btn = findViewById(id);
        gameManager.submitAnswerForCurrentQuestion((int) btn.getTag());
    }
}



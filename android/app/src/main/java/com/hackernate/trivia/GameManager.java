package com.hackernate.trivia;

import com.google.gson.Gson;
import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.Question;
import com.hackernate.trivia.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Socket;

public class GameManager {
    private Socket socket;
    private String gameId;
    private Game game;
    private User currentUser;
    private Listener listener;
    private Gson gson = new Gson();

    interface Listener {
        public void gameLoaded();

        public void gameUpdated();

        public void updatedWithPlayerScores(String userId);

        public void updatedWithNextQuestion();
    }

    public GameManager(String gameId, User currentUser, Socket socket, Listener listener) {
        assert (gameId != null);
        this.gameId = gameId;
        this.currentUser = currentUser;
        this.socket = socket;
        this.listener = listener;
        loadGame();
        setupListeners();
    }

    public Game getGame() {
        return game;
    }

    private void loadGame() {
        socket.emit("game:fetch", new Object[] { gameId }, (args) -> {
            game = new Gson().fromJson(args[0].toString(), Game.class);
            listener.gameLoaded();
        });
    }

    public void submitAnswerForCurrentQuestion(int answerIndex) {
        socket.emit("game:submitAnswer", gameId, answerIndex);
    }

    public void startGame() {
        socket.emit("game:start", gameId);
    }

    private void setupListeners() {
        socket.on("game:question", (args) -> {
            if (game == null) return;
            String gameId = (String) args[0];
            if (!this.gameId.equals(gameId)) {
                return;
            }
            Question question = gson.fromJson((String) args[1], Question.class);
            System.out.println("game:question event. question=" + question);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("updating game with question");
                    game.questions.add(question);
                    listener.updatedWithNextQuestion();
                }
            }, 500);
        });

        socket.on("game:playerAnswer", (args) -> {
            if (game == null) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String gameId = data.getString("gameId");

                if (!this.gameId.equals(gameId)) {
                    return;
                }

                String userId = data.getString("userId");
                List<Integer> responses = new ArrayList<>();
                for (int i = 0; i < data.getJSONArray("responses").length(); i++) {
                    responses.add((int) data.getJSONArray("responses").get(i));
                }
                game.setPlayerResponses(userId, responses);

                listener.updatedWithPlayerScores(userId);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        socket.on("game:player.join", (args) -> {
            if (game == null) return;
            String gameId = (String) args[0];
            if (!this.gameId.equals(gameId)) {
                return;
            }
            User user = gson.fromJson((String) args[1], User.class);
            System.out.println("PLAYER JOIN: " + user);
            game.addPlayer(user);
            listener.gameUpdated();
        });

        socket.on("game:starting", (args) -> {
            if (game == null) return;
            String gameId = (String) args[0];
            if (!this.gameId.equals(gameId)) {
                return;
            }
            System.out.println("GAME START!");
            game.setAsInPlay();
        });

        socket.on("game:done", (args) -> {
            String gameId = (String) args[0];
            if (!this.gameId.equals(gameId)) {
                return;
            }
            System.out.println("GAME OVER!");
            loadGame();
        });
    }

    public void stopListeners() {
        socket.off("game:question");
        socket.off("game:player.join");
        socket.off("game:starting");
    }
}

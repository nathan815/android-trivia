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
import io.socket.emitter.Emitter;

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
        startListeners();
    }

    public Game getGame() {
        return game;
    }

    private void loadGame() {
        socket.emit("game:fetch", new Object[]{gameId}, (args) -> {
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

    private void startListeners() {
        socket.on("game:player.join", playerJoinListener);
        socket.on("game:player.answer", playerAnswerListener);
        socket.on("game:starting", gameStartingListener);
        socket.on("game:question", questionListener);
        socket.on("game:done", gameDoneListener);
    }

    public void stopListeners() {
        socket.off("game:player.join");
        socket.off("game:player.answer");
        socket.off("game:starting");
        socket.off("game:question");
        socket.off("game:done");
    }

    private Emitter.Listener questionListener = (args) -> {
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
    };

    private Emitter.Listener playerAnswerListener = (args) -> {
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
    };

    private Emitter.Listener playerJoinListener = (args) -> {
        if (game == null) return;
        String gameId = (String) args[0];
        if (!this.gameId.equals(gameId)) {
            return;
        }
        User user = gson.fromJson((String) args[1], User.class);
        System.out.println("PLAYER JOIN: " + user);
        game.addPlayer(user);
        listener.gameUpdated();
    };

    private Emitter.Listener gameStartingListener = (args) -> {
        if (game == null) return;
        String gameId = (String) args[0];
        if (!this.gameId.equals(gameId)) {
            return;
        }
        System.out.println("GAME START!");
        game.setAsInPlay();
    };

    private Emitter.Listener gameDoneListener = (args) -> {
        String gameId = (String) args[0];
        if (!this.gameId.equals(gameId)) {
            return;
        }
        System.out.println("GAME OVER!");
        loadGame();
    };
}

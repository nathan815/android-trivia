package com.hackernate.trivia;

import com.google.gson.Gson;
import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.Question;
import com.hackernate.trivia.data.User;

import io.socket.client.Socket;

public class GameManager {
    private Socket socket;
    private String gameId;
    private Game game;
    private Listener listener;
    private Gson gson = new Gson();

    interface Listener {
        public void gameLoaded();
        public void gameUpdated();
    }

    public GameManager(String gameId, Socket socket, Listener listener) {
        assert(gameId != null);
        this.gameId = gameId;
        this.socket = socket;
        this.listener = listener;
        loadGame();
        setupListeners();
    }

    public Game getGame() {
        return game;
    }

    private void loadGame() {
        socket.emit("game:fetch", gameId);
        socket.once("game:fetch.response", (args) -> {
            game = new Gson().fromJson(args[0].toString(), Game.class);
            listener.gameLoaded();
        });
    }

    public void startGame() {
        socket.emit("game:start", gameId);
    }

    private void setupListeners() {
        socket.on("game:question", (args) -> {
            String gameId = (String)args[0];
            if(!this.gameId.equals(gameId)) {
                return;
            }
            Question question = gson.fromJson((String)args[1], Question.class);
            System.out.println("question: " + question);
            game.questions.add(question);
            listener.gameUpdated();
        });

        socket.on("game:player.join", (args) -> {
            String gameId = (String)args[0];
            if(!this.gameId.equals(gameId)) {
                return;
            }
            User user = gson.fromJson((String)args[1], User.class);
            System.out.println("PLAYER JOIN: " + user);
            game.addPlayer(user);
            listener.gameUpdated();
        });

        socket.on("game:starting", (args) -> {
            System.out.println("GAME START!");
            game.start();
        });
    }

    public void stopListeners() {
        socket.off("game:question");
        socket.off("game:player.join");
        socket.off("game:starting");
    }
}

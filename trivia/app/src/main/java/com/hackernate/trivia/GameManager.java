package com.hackernate.trivia;

import com.google.gson.Gson;
import com.hackernate.trivia.data.Game;

import io.socket.client.Socket;

public class GameManager {
    private Socket socket;
    private String gameId;
    private Game game;
    private Listener listener;

    interface Listener {
        public void gameLoaded();
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

    private void setupListeners() {
        socket.on("game:newQuestion", (args)->{});
    }
}

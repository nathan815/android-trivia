package com.hackernate.trivia;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hackernate.trivia.data.User;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.socket.client.Socket;

public class MainManager {
    private static MainManager instance;

    private Socket socket;
    private Gson gson = new Gson();
    private SharedPreferences sharedPref;

    private MainManager(Socket socket, SharedPreferences sharedPref) {
        this.socket = socket;
        this.sharedPref = sharedPref;
        listeners();
    }

    public static MainManager getInstance(TriviaApplication application) {
        if(instance == null) {
            instance = new MainManager(application.getSocket(), application.getSharedPrefs());
        }
        return instance;
    }

    private void listeners() {

    }

    public void userEntered(User user) {
        socket.emit("user:enter", gson.toJson(user));
    }

    public User getSavedUser() {
        try {
            String userJson = sharedPref.getString("user", "");
            return gson.fromJson(userJson, User.class);
        } catch(JsonSyntaxException e) {
            return null;
        }
    }

    public void setSavedUser(User user) {
        sharedPref.edit().putString("user", gson.toJson(user)).apply();
    }

    public void createGame(String name, Consumer<String> callback) {
        socket.emit("game:create", name);
        socket.once("game:created", (args) -> {
            callback.accept((String)args[0]);
        });
    }

    public void joinGame(String id, BiConsumer<Boolean, String> callback) {
        socket.emit("game:join", id);
        socket.once("game:joined", (args) -> callback.accept(true, ""));
        socket.once("game:joined.error", (args) -> callback.accept(false, (String)args[0]));
    }
}

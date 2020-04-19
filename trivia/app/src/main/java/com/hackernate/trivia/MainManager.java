package com.hackernate.trivia;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.hackernate.trivia.data.Game;
import com.hackernate.trivia.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
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
        socketListeners();
    }

    public static MainManager getInstance(TriviaApplication application) {
        if(instance == null) {
            instance = new MainManager(application.getSocket(), application.getSharedPrefs());
        }
        return instance;
    }

    private void socketListeners() {
        socket.on(Socket.EVENT_RECONNECT, (args) -> {
            userEntered(getSavedUser());
        });
    }

    public void userEntered(User user) {
        try {
            socket.emit("user:enter", new JSONObject(gson.toJson(user)));
        } catch(JSONException e) {}
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
        socket.once("game:joined", (args) -> {
            socket.off("game:join.error");
            callback.accept(true, null);
        });
        socket.once("game:join.error", (args) -> {
            System.out.println("error: "+ args[0]);
            socket.off("game:joined");
            callback.accept(false, (String)args[0]);
        });
    }

    public void getMyGames(Consumer<List<Game>> callback) {
        socket.emit("game:list");
        socket.once("game:list.response", (args) -> {
            Type listType = new TypeToken<ArrayList<Game>>(){}.getType();
            List<Game> games = gson.fromJson((String)args[0], listType);
            callback.accept(games);
        });
    }
}

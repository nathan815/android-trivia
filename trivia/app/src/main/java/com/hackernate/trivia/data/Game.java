package com.hackernate.trivia.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Game {

    public String name;

    @SerializedName("_id")
    public String id;
    private String status;

    private List<User> players;
    public List<Question> questions;

    public boolean isWaitingToStart() {
        return status.equals("waiting");
    }
    public boolean hasStarted() {
        return status.equals("inplay");
    }


    public List<User> getPlayers() {
        return players;
    }

    public boolean canBeStarted() {
        return players.size() >= 2;
    }

    public int playersCount()  {
        return players.size();
    }
    public void submitAnswer(String userId, int answerIndex) {

    }
}

package com.hackernate.trivia.data;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Game {

    public String name;

    @SerializedName("_id")
    public String id;
    private String status;

    private String ownerId;

    private Map<String, User> players;

    public List<Question> questions;

    public String getOwnerId() {
        return ownerId;
    }

    public User getOwner() {
        return players.get(ownerId);
    }

    public boolean isWaitingToStart() {
        return status.equals("waiting");
    }
    public boolean hasStarted() {
        return status.equals("inplay");
    }

    public Question getLatestQuestion() {
        return questions.get(questions.size()-1);
    }

    public Collection<User> getPlayers() {
        return players.values();
    }

    public void addPlayer(User player) {
        this.players.put(player.id, player);
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

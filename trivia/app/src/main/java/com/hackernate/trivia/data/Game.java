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

    private Map<String, List<Integer>> playerResponses;

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

    public void setPlayerResponses(String userId, List<Integer> responses) {
        playerResponses.put(userId, responses);
    }

    public void start() {
        status = "inplay";
    }

    public Question getLatestQuestion() {
        return questions.size() == 0 ? null : questions.get(questions.size()-1);
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

    @Override
    public String toString() {
        return "Game{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", playerResponses=" + playerResponses +
                ", players=" + players +
                ", questions=" + questions +
                '}';
    }
}

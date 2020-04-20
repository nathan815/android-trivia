package com.hackernate.trivia.data;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.HashMap;
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

    public boolean isDone() {
        return status.equals("done");
    }

    public void setPlayerResponses(String userId, List<Integer> responses) {
        playerResponses.put(userId, responses);
    }

    public int askedQuestionsCount() {
        return questions.size();
    }

    public int maxQuestionCount() {
        return 10;
    }

    public int getCurrentSelectedAnswer(String userId) {
        List<Integer> responses = playerResponses.getOrDefault(userId, null);

        // check if user has not selected answer yet
        if (responses == null || responses.size() < questions.size()) {
            return -1;
        }

        return responses.get(responses.size() - 1);
    }

    public Map<String, List<Integer>> getPlayerResponses() {
        return playerResponses;
    }

    public Map<User, Integer> getPlayerPointsMap() {
        Map<User, Integer> map = new HashMap<>();
        for (User player : getPlayers()) {
            map.put(player, calculatePlayerPoints(player.id));
        }
        return map;
    }
    private int calculatePlayerPoints(String userId) {
        List<Integer> responses = playerResponses.get(userId);
        if(responses == null) {
            return 0;
        }
        int points = 0;
        for (int qIndex = 0; qIndex < questions.size() && qIndex < responses.size(); qIndex++) {
            int answerIndex = responses.get(qIndex);
            Question question = questions.get(qIndex);
            if(answerIndex != -1) {
                boolean isCorrect = answerIndex == question.getCorrectIndex();
                if(isCorrect) {
                    points += question.getPoints();
                }
            }
        }
        return points;
    }

    public void setAsInPlay() {
        status = "inplay";
    }

    public Question getLatestQuestion() {
        return questions.size() == 0 ? null : questions.get(questions.size() - 1);
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

    public int playersCount() {
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

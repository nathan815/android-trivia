package com.hackernate.trivia.data;

import java.util.List;

public class Question {

    private String question;
    private List<String> answers;
    private int correctIndex;
    private int points;

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public int getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "Question{" +
                "question='" + question + '\'' +
                ", answers=" + answers +
                ", correctIndex=" + correctIndex +
                '}';
    }
}

package com.hackernate.trivia.data;

import java.util.List;

public class Question {
    private String text;
    private List<String> answers;
    private int correctIndex;

    public String getText() {
        return text;
    }

    public List<String> getAnswers() {
        return answers;
    }
}

package com.hackernate.trivia.data;


public class User {
    public String id;
    public String username;

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}

package com.hackernate.trivia.data;


import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
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

package com.example.mywatchapp;

public class UserProfile {
    public String uid;
    public String name;
    public String gender;
    public String age;
    public String photoUri;

    public UserProfile(String uid, String name, String gender, String age, String photoUri) {
        this.uid = uid;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.photoUri = photoUri;
    }
}

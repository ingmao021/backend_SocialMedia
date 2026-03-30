package com.example.backend_socialmedia.auth.domain;

public class User {
    private Long id;
    private String name;
    private String email;
    private String picture;
    private String googleId;

    public User() {}

    public User(Long id, String name, String email, String picture, String googleId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.googleId = googleId;
    }

    // Getters y Setters
    public Long getId()              { return id; }
    public void setId(Long id)       { this.id = id; }
    public String getName()          { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail()         { return email; }
    public void setEmail(String email){ this.email = email; }
    public String getPicture()       { return picture; }
    public void setPicture(String p) { this.picture = p; }
    public String getGoogleId()      { return googleId; }
    public void setGoogleId(String g){ this.googleId = g; }
}
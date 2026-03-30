package com.example.backend_socialmedia.auth.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String picture;

    @Column(name = "google_id", unique = true)
    private String googleId;

    // Getters y Setters
    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }
    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }
    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }
    public String getPicture()           { return picture; }
    public void setPicture(String p)     { this.picture = p; }
    public String getGoogleId()          { return googleId; }
    public void setGoogleId(String g)    { this.googleId = g; }
}

package com.seichou.logos.dto;

public class AuthUserDto {
    private String id;
    private String email;
    private String name;
    private String bio;
    private String role;
    private String joinedAt;

    public AuthUserDto() {
    }

    public AuthUserDto(String id, String email, String name, String bio, String role, String joinedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.bio = bio;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
}
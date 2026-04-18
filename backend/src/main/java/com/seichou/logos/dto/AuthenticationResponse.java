package com.seichou.logos.dto;

public class AuthenticationResponse {
    private String token;
    private AuthUserDto user;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(String token, AuthUserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public AuthUserDto getUser() { return user; }
    public void setUser(AuthUserDto user) { this.user = user; }
}

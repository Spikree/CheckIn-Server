package com.learnspring.checkinserver.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private Long id;
    private String username;
    private String role;
    private String token;

    public AuthResponse(Long id, String username, String role,  String token) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.token = token;
    }
}
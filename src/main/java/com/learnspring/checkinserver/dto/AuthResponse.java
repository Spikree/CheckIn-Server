package com.learnspring.checkinserver.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private Long id;
    private String username;
    private String role;

    public AuthResponse(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
}
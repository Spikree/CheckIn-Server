package com.learnspring.checkinserver.payload;

import lombok.Data;

@Data
public class TaskRequest {
    private String title;
    private String description;
}
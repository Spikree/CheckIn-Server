package com.learnspring.checkinserver.dto;

import lombok.Data;

@Data
public class TaskDto {
    private Long id;
    private String title;
    private String description;
    private boolean isCompleted;

    public TaskDto(Long id, String title ,String description, boolean isCompleted) {
        this.id = id;

        this.title = title;
        this.description = description;
        this.isCompleted = isCompleted;
    }
}
package com.learnspring.checkinserver.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "task_logs")
public class TaskLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Task task;

    @Column(nullable = false)
    private LocalDate date;
}

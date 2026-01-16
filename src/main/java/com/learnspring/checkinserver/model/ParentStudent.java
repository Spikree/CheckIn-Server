package com.learnspring.checkinserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parent_student_links")
public class ParentStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester; // Who sent the friend request?

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // Who needs to accept it?

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, ACCEPTED, REJECTED

    private LocalDateTime createdAt;

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    public ParentStudent() {}

    public ParentStudent(User requester, User receiver) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = Status.PENDING; // Default to pending
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
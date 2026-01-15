package com.learnspring.checkinserver.controller;

import com.learnspring.checkinserver.model.Task;
import com.learnspring.checkinserver.model.TaskLog;
import com.learnspring.checkinserver.model.User;
import com.learnspring.checkinserver.repository.TaskLogRepository;
import com.learnspring.checkinserver.repository.TaskRepository;
import com.learnspring.checkinserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private UserRepository userRepository;

    // --- 1. Create Task ---
    @PostMapping("/users/{userId}/tasks")
    public Task createTask(@PathVariable long userId, @RequestBody String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Task task = new Task();
        task.setDescription(description);
        task.setActive(true);
        task.setUser(user);

        return taskRepository.save(task);
    }

    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<?> getTasksForUser(@PathVariable Long userId, Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String sessionEmail = userDetails.getUsername();

        User currentUser = userRepository.findByEmail(sessionEmail)
                .orElseThrow(() -> new RuntimeException("Logged in user not found in DB"));


        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body("Access Denied: You cannot view another user's tasks.");
        }

        List<Task> tasks = taskRepository.findByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    // --- 3. Toggle Task ---
    @PostMapping("/users/{userId}/tasks/{taskId}/toggle")
    public String toggleTask(@PathVariable Long userId, @PathVariable Long taskId, Principal principal) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(userId)) {
            throw new RuntimeException("ID Mismatch");
        }

        if (!task.getUser().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Unauthorized: You do not own this task!");
        }

        LocalDate today = LocalDate.now();
        Optional<TaskLog> existingLog = taskLogRepository.findByTaskIdAndDate(taskId, today);

        if (existingLog.isPresent()) {
            taskLogRepository.delete(existingLog.get());
            return "Task Unchecked";
        } else {
            TaskLog log = new TaskLog();
            log.setTask(task);
            log.setDate(today);
            taskLogRepository.save(log);
            return "Task Checked";
        }
    }
}
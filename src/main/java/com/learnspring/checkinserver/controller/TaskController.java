package com.learnspring.checkinserver.controller;

import com.learnspring.checkinserver.model.Task;
import java.security.Principal;
import com.learnspring.checkinserver.model.TaskLog;
import com.learnspring.checkinserver.model.User;
import com.learnspring.checkinserver.repository.TaskLogRepository;
import com.learnspring.checkinserver.repository.TaskRepository;
import com.learnspring.checkinserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.learnspring.checkinserver.dto.TaskDto;

import java.time.LocalDate;
import java.util.ArrayList;
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

    @PostMapping("/users/{userId}/tasks")
    public Task createTask(@PathVariable long userId, @RequestBody String description) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Task task = new Task();
        task.setDescription(description);
        task.setActive(true);
        task.setUser(user);

        return taskRepository.save(task);
    }

    @GetMapping("/users/{userId}/tasks")
    public List<TaskDto> getTasksForUser(@PathVariable Long userId, Principal principal) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // FIX 2: Security Check - Stop here if unauthorized
        if (!user.getUsername().equals(principal.getName())) {
            throw new RuntimeException("Unauthorized user to view this task");
        }
        // FIX 3: Close the IF block here! The rest of the code runs only if we pass the check.

        // --- Normal Logic Starts Here ---
        List<Task> tasks = taskRepository.findByUserId(userId);
        List<TaskDto> response = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Task task : tasks) {
            boolean isDone = taskLogRepository.existsByTaskIdAndDate(task.getId(), today);
            response.add(new TaskDto(task.getId(), task.getDescription(), isDone));
        }

        return response;
    }

    @PostMapping("/users/{userId}/tasks/{taskId}/toggle")
    public String toggleTask(@PathVariable Long userId, @PathVariable Long taskId, Principal principal) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 1. Check if Task matches URL User (The check we already had)
        if (!task.getUser().getId().equals(userId)) {
            throw new RuntimeException("ID Mismatch");
        }

        // 2. NEW SECURITY: Check if Logged-in User (Principal) owns the task
        if (!task.getUser().getUsername().equals(principal.getName())) {
            throw new RuntimeException("Unauthorized: You do not own this task!");
        }

        // 3. Normal logic...
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

package com.learnspring.checkinserver.controller;

import com.learnspring.checkinserver.dto.TaskDto;
import com.learnspring.checkinserver.model.Role;
import com.learnspring.checkinserver.model.Task;
import com.learnspring.checkinserver.model.TaskLog;
import com.learnspring.checkinserver.model.User;
import com.learnspring.checkinserver.payload.TaskRequest;
import com.learnspring.checkinserver.repository.ParentStudentRepository;
import com.learnspring.checkinserver.repository.TaskLogRepository;
import com.learnspring.checkinserver.repository.TaskRepository;
import com.learnspring.checkinserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @Autowired
    private ParentStudentRepository parentStudentRepository;

    @PostMapping("/users/{userId}/tasks")
    public ResponseEntity<?> createTask(@PathVariable long userId,
                                        @RequestBody TaskRequest taskRequest,
                                        Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User actor = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Actor not found"));

        Optional<User> targetOptional = userRepository.findById(userId);
        if (targetOptional.isEmpty()) {
            return ResponseEntity.status(404).body("Error: Target user not found.");
        }
        User target = targetOptional.get();

        boolean isSelf = actor.getId().equals(target.getId());
        boolean isParent = actor.getRole() == Role.PARENT;
        boolean isLinked = parentStudentRepository.areUsersLinked(actor.getId(), target.getId());

        if (isSelf || (isParent && isLinked)) {
            Task task = new Task();

            task.setTitle(taskRequest.getTitle());
            task.setDescription(taskRequest.getDescription());

            task.setActive(true);
            task.setUser(target);

            if (isSelf) {
                task.setAssignedBy("Self");
            } else {
                task.setAssignedBy(actor.getUsername());
            }

            Task savedTask = taskRepository.save(task);
            return ResponseEntity.ok(savedTask);
        }

        return ResponseEntity.status(403).body("Error: Unauthorized.");
    }

    // this is for parents to get their childrens task
    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<?> getTasksForUser(@PathVariable Long userId, Authentication authentication) {

        // 1. Get Logged-in User (The "Viewer")
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User viewer = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Identify the Target (The "Owner" of the tasks)
        Long targetUserId = userId;

        // RULE 1: If I am looking at my own tasks -> ALLOW
        if (viewer.getId().equals(targetUserId)) {
            return ResponseEntity.ok(taskRepository.findByUserId(targetUserId));
        }

        // RULE 2: If I am a PARENT looking at a Linked Student -> ALLOW
        boolean isParent = viewer.getRole() == Role.PARENT; // Check if Viewer is a Parent
        boolean isLinked = parentStudentRepository.areUsersLinked(viewer.getId(), targetUserId);

        if (isParent && isLinked) {
            return ResponseEntity.ok(taskRepository.findByUserId(targetUserId));
        }

        // BLOCK everything else (Students looking at Parents, Strangers, etc.)
        return ResponseEntity.status(403).body("Access Denied: You are not authorized to view these tasks.");
    }


    // this is for students to get their tasks
    // GET /api/tasks
    @GetMapping("/tasks")
    public ResponseEntity<?> getMyOwnTasks(Authentication authentication) {

        // 1. Who is logged in?
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Fetch and return ONLY their tasks. No security checks needed
        // because we are strictly using the token identity!
        return ResponseEntity.ok(taskRepository.findByUserId(me.getId()));
    }

    // --- 3. Toggle Task ---
    // 3. Toggle Task (Strictly Owner Only)
    @PostMapping("/tasks/{taskId}/toggle")
    @Transactional
    public String toggleTask(@PathVariable Long taskId, Principal principal) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // RULE: Only the TASK OWNER can tick it off.
        if (!task.getUser().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Unauthorized: Only the assigned student can complete this task!");
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

    @GetMapping("/tasks/completed")
    public ResponseEntity<?> getMyDailyTasks(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Task> myTasks = taskRepository.findByUserId(me.getId());
        LocalDate today = LocalDate.now();

        List<TaskDto> responseList = new ArrayList<>();

        for (Task task : myTasks) {
            boolean isDoneToday = taskLogRepository.findByTaskIdAndDate(task.getId(), today).isPresent();

            if (isDoneToday) {
                responseList.add(new TaskDto(
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),
                        true, // It's obviously true if we are in this block
                        task.getAssignedBy()
                ));
            }
        }

        return ResponseEntity.ok(responseList);
    }

    // This controller is for parents to get the tasks the students have completed today
    // GET /api/users/{userId}/tasks/completed
    @GetMapping("/users/{userId}/tasks/completed")
    public ResponseEntity<?> getCompletedTasksForStudent(@PathVariable Long userId, Authentication authentication) {

        // 1. Get the Parent (The one making the request)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User parent = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Parent not found"));

        // 2. Security Check: Is this person actually a parent and linked to this student?
        boolean isParent = parent.getRole() == Role.PARENT;
        boolean isLinked = parentStudentRepository.areUsersLinked(parent.getId(), userId);

        if (!isParent || !isLinked) {
            return ResponseEntity.status(403).body("Access Denied: You are not linked to this student.");
        }

        // 3. Logic: Get the student's tasks and check today's logs
        List<Task> studentTasks = taskRepository.findByUserId(userId);
        LocalDate today = LocalDate.now();
        List<TaskDto> completedToday = new ArrayList<>();

        for (Task task : studentTasks) {
            boolean isDoneToday = taskLogRepository.findByTaskIdAndDate(task.getId(), today).isPresent();

            if (isDoneToday) {
                completedToday.add(new TaskDto(
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),

                        true,
                        task.getAssignedBy()
                ));
            }
        }

        return ResponseEntity.ok(completedToday);
    }

    // This controller is to get all the previous tasks for both the parents and the students
    @GetMapping("/tasks/{taskId}/history")
    public ResponseEntity<?> getTaskHistory(@PathVariable Long taskId, Authentication authentication) {

        // 1. Who is making this request?
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User requester = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. What task are they asking about, and who owns it?
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User taskOwner = task.getUser();

        // 3. Security Check Variables
        boolean isOwner = requester.getId().equals(taskOwner.getId());
        boolean isParent = requester.getRole() == Role.PARENT;
        boolean isLinkedParent = isParent && parentStudentRepository.areUsersLinked(requester.getId(), taskOwner.getId());

        // 4. The Vault Door: Deny if they are neither the owner nor a linked parent
        if (!isOwner && !isLinkedParent) {
            return ResponseEntity.status(403)
                    .body("Access Denied: You are not authorized to view this task's history.");
        }

        // 5. If they pass the check, fetch the logs
        List<TaskLog> history = taskLogRepository.findByTaskId(taskId);

        // 6. Map them to a list of dates
        List<LocalDate> completedDates = history.stream()
                .map(TaskLog::getDate)
                .toList();

        return ResponseEntity.ok(completedDates);
    }
}
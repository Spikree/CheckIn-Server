package com.learnspring.checkinserver.controller;

import com.learnspring.checkinserver.model.ParentStudent;
import com.learnspring.checkinserver.model.Role;
import com.learnspring.checkinserver.model.User;
import com.learnspring.checkinserver.repository.ParentStudentRepository;
import com.learnspring.checkinserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

    // POST /api/family/request?targetId=5
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestParam Long targetId, Authentication authentication) {
        UserDetails meDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(meDetails.getUsername()).orElseThrow();

        // FIX: Find by ID instead of Email
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("User with ID " + targetId + " not found"));


        if (me.getRole() == Role.STUDENT && target.getRole() == Role.STUDENT) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Students cannot link with other Students. Ask a Parent to send the request.");
        }

        if (me.getId().equals(target.getId())) {
            return ResponseEntity.badRequest().body("You cannot link with yourself.");
        }

        // Check if link already exists (in either direction)
        boolean exists = parentStudentRepository.existsByRequesterIdAndReceiverId(me.getId(), target.getId()) ||
                parentStudentRepository.existsByReceiverIdAndRequesterId(me.getId(), target.getId());

        if (exists) {
            return ResponseEntity.badRequest().body("Request already exists or you are already linked.");
        }

        ParentStudent request = new ParentStudent(me, target);
        parentStudentRepository.save(request);

        return ResponseEntity.ok("Request sent to User ID: " + target.getId());
    }

    // GET /api/family/requests
    @GetMapping("/requests")
    public ResponseEntity<List<ParentStudent>> getMyPendingRequests(Authentication authentication) {
        UserDetails meDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(meDetails.getUsername()).orElseThrow();

        // Fetch all PENDING requests where I am the RECEIVER
        List<ParentStudent> requests = parentStudentRepository.findByReceiverIdAndStatus(
                me.getId(), ParentStudent.Status.PENDING
        );
        return ResponseEntity.ok(requests);
    }

    // POST /api/family/requests/{requestId}/accept
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long requestId, Authentication authentication) {
        UserDetails meDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(meDetails.getUsername()).orElseThrow();

        // Find the request ONLY if it belongs to me (Security Check!)
        ParentStudent request = parentStudentRepository.findByIdAndReceiverId(requestId, me.getId())
                .orElseThrow(() -> new RuntimeException("Request not found or not yours"));

        request.setStatus(ParentStudent.Status.ACCEPTED);
        parentStudentRepository.save(request);

        return ResponseEntity.ok("You are now linked with " + request.getRequester().getUsername());
    }

    // GET /api/family/requests/sent
    @GetMapping("/requests/sent")
    public ResponseEntity<List<ParentStudent>> getMySentRequests(Authentication authentication) {
        UserDetails meDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(meDetails.getUsername()).orElseThrow();

        List<ParentStudent> sentRequests = parentStudentRepository.findByRequesterIdAndStatus(
                me.getId(), ParentStudent.Status.PENDING
        );
        return ResponseEntity.ok(sentRequests);
    }

    // DELETE /api/family/requests/{requestId}
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<?> cancelRequest(@PathVariable Long requestId, Authentication authentication) {
        UserDetails meDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(meDetails.getUsername()).orElseThrow();

        // Security: Can only delete if I am the REQUESTER
        ParentStudent request = parentStudentRepository.findByIdAndRequesterId(requestId, me.getId())
                .orElseThrow(() -> new RuntimeException("Request not found or you are not the sender"));

        parentStudentRepository.delete(request);

        return ResponseEntity.ok("Request cancelled successfully.");
    }

    // GET /api/family/children
    @GetMapping("/children")
    public ResponseEntity<?> getMyChildren(Authentication authentication) {
        UserDetails meDetails = (UserDetails) authentication.getPrincipal();
        User me = userRepository.findByEmail(meDetails.getUsername()).orElseThrow();

        // --- SECURITY CHECK START ---
        if (me.getRole() != Role.PARENT) {
            return ResponseEntity.status(403).body("Only parents can view registered children.");
        }

        // 1. Get all my accepted links
        List<ParentStudent> links = parentStudentRepository.findAllAcceptedLinks(me.getId());

        List<User> children = new ArrayList<>();

        for (ParentStudent link : links) {
            User otherPerson;

            // Logic to find the "Other" person
            if (link.getRequester().getId().equals(me.getId())) {
                otherPerson = link.getReceiver();
            } else {
                otherPerson = link.getRequester();
            }

            // 2. Filter: Only add them if they are a STUDENT
            if (otherPerson.getRole() == Role.STUDENT) {
                children.add(otherPerson);
            }
        }

        return ResponseEntity.ok(children);
    }
}
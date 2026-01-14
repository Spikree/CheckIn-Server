package com.learnspring.checkinserver.controller;

import com.learnspring.checkinserver.dto.AuthResponse;
import com.learnspring.checkinserver.dto.LoginRequest;
import com.learnspring.checkinserver.model.User;
import com.learnspring.checkinserver.repository.UserRepository;
import com.learnspring.checkinserver.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;      // Was missing

    @Autowired
    private PasswordEncoder passwordEncoder;    // Was missing


    // --- SIGNUP FUNCTION (With Auto-Login) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        // 1. Check if email exists (Better to check email since we login with it)
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
        }

        // 2. Encrypt the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. Save to DB
        User savedUser = userRepository.save(user);

        // 4. Generate Token immediately
        String token = jwtUtils.generateToken(savedUser.getEmail());

        // 5. Return the Token + ID (Just like Login!)
        return ResponseEntity.ok(new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name(),
                token
        ));
    }

    // --- LOGIN FUNCTION ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1. Authenticate (This checks email/password against DB)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        // 2. If valid, generate Token
        if (authentication.isAuthenticated()) {
            // Note: loginRequest.getUsername() actually holds the EMAIL now
            User user = userRepository.findByEmail(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtils.generateToken(user.getEmail());

            // 3. Return Response
            return ResponseEntity.ok(new AuthResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    token
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credentials");
        }
    }
}
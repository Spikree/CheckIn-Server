package com.learnspring.checkinserver.controller;

import com.learnspring.checkinserver.dto.AuthResponse;
import com.learnspring.checkinserver.dto.LoginRequest;
import com.learnspring.checkinserver.model.Role;
import com.learnspring.checkinserver.model.User;
import com.learnspring.checkinserver.repository.UserRepository;
import com.learnspring.checkinserver.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

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
    public ResponseEntity<?> registerUser(@RequestBody User user, HttpServletResponse response) {

        // 1. CHECK: Is Username taken?
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity
                    .status(400)
                    .body("Error: Username '" + user.getUsername() + "' is already taken!");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity
                    .status(400)
                    .body("Error: Email '" + user.getEmail() + "' is already in use!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) {
            return ResponseEntity
                    .status(400)
                    .body("Role is required!");
        }

        // 5. Save to Database
        User savedUser = userRepository.save(user);

        // A. Generate Token
        String token = jwtUtils.generateToken(savedUser.getEmail());

        // B. Create Cookie
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in Production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(10 * 60 * 60); // 10 Hours

        // C. Attach Cookie to Response
        response.addCookie(cookie);

        // 6. Return Success
        return ResponseEntity.ok(new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name()
        ));
    }

    // --- LOGIN FUNCTION ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            // 1. Attempt Authentication (This THROWS an exception if it fails)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // 2. If we get here, the password was correct!
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3. Generate Token
            String token = jwtUtils.generateToken(user.getEmail());

            // 4. Create Cookie
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true for HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(10 * 60 * 60);
            response.addCookie(cookie);

            // 5. Return Success
            return ResponseEntity.ok(new AuthResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid email or password");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login Error: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        // 1. Create a "Death Cookie" (Same name, same path, but 0 lifespan)
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Match your login settings
        cookie.setPath("/");     // Must match the login path
        cookie.setMaxAge(0);     // 0 = Delete Immediately

        // 2. Send it to the browser
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> checkAuth(Authentication authentication) {
        // 1. If the request made it past the JwtFilter, they are authenticated!
        // But let's double-check just to be safe.
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        // 2. Get the user's email from the security context
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Look them up in the database to get their ID and Role
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 4. Return the exact same AuthResponse we use for Login!
        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        ));
    }
}
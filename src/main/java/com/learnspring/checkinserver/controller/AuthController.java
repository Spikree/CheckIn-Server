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
        // 1. Check if email exists (Better to check email since we login with it)
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
        }

        // 2. Encrypt the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);


        // 4. Generate Token immediately
        String token = jwtUtils.generateToken(user.getEmail());


        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);       // Prevents JavaScript from reading it
        cookie.setSecure(false);        // Set to TRUE if using HTTPS
        cookie.setPath("/");            // Available for the whole app
        cookie.setMaxAge(10 * 60 * 60);

        response.addCookie(cookie);

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
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Generate Token
            String token = jwtUtils.generateToken(user.getEmail());

            // 3. CREATE THE COOKIE 🍪
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);       // Prevents JavaScript from reading it
            cookie.setSecure(false);        // Set to TRUE if using HTTPS
            cookie.setPath("/");            // Available for the whole app
            cookie.setMaxAge(10 * 60 * 60);

            // 4. Add Cookie to Response
            response.addCookie(cookie);

            return ResponseEntity.ok(new AuthResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    null
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credentials");
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
}
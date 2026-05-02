package com.yourform.formbuilder.controller;

import com.yourform.formbuilder.model.User;
import com.yourform.formbuilder.repository.UserRepository;
import com.yourform.formbuilder.security.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repo;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository repo, JwtUtil jwtUtil) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Username and password are required");
        }

        if (repo.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        }

        repo.save(user);

        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        if (isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Username and password are required");
        }

        User dbUser = repo.findByUsername(user.getUsername()).orElse(null);

        if (dbUser == null || !dbUser.getPassword().equals(user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        return ResponseEntity.ok(jwtUtil.generateToken(user.getUsername()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody User user) {
        if (isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Username and new password are required");
        }

        User dbUser = repo.findByUsername(user.getUsername()).orElse(null);

        if (dbUser == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Username not found");
        }

        dbUser.setPassword(user.getPassword());
        repo.save(dbUser);

        return ResponseEntity.ok("Password updated. Use Login to continue.");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

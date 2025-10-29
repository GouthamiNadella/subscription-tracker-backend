package com.example.subscriptiontracker.controller;

import com.example.subscriptiontracker.dto.LoginRequest;
import com.example.subscriptiontracker.dto.RegisterRequest;
import com.example.subscriptiontracker.dto.UserResponse;
import com.example.subscriptiontracker.model.User;
import com.example.subscriptiontracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request){
        log.info("Registration request received for email: {}", request.getEmail());

        try {
            User user = userService.registerUser(
                    request.getEmail(),
                    request.getName(),
                    request.getPassword()
            );

            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .emailNotifications(user.isEmailNotifications())
                    .createdAt(user.getCreatedAt())
                    .build();

            log.info("User successfully registered with ID: {}", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        try {
            User user = userService.authenticateUser(
                    request.getEmail(),
                    request.getPassword()
            );

            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .emailNotifications(user.isEmailNotifications())
                    .createdAt(user.getCreatedAt())
                    .build();

            log.info("User {} successfully authenticated", user.getEmail());
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }

    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId){
        log.info("Profile request received for user ID: {}", userId);

        try {
            User user = userService.getUserById(userId);

            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .emailNotifications(user.isEmailNotifications())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get profile for user {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

}

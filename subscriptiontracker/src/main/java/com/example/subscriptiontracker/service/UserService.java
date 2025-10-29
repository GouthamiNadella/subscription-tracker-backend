package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.model.User;
import com.example.subscriptiontracker.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(String email, String name, String rawPassword) {
        log.info("Attempting to register user with email: {}", email);

        if(userRepository.existsByEmail(email)) {
            log.warn("Registration failed: Email {} already exists", email);
            throw new RuntimeException("User with email " + email + " already exists");
        }

        String encryptedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .email(email)
                .name(name)
                .password(encryptedPassword)
                .emailNotifications(true)
                .build();
        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {}", savedUser.getId());

        return savedUser;
    }

    public User authenticateUser(String email, String rawPassword) {
        log.info("Authentication attempt for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        if(userOpt.isEmpty()){
            log.warn("Authentication failed: User not found for email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();

        if(!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Authentication failed: Invalid password for email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }

        log.info("Successfully authenticated user with ID: {}", user.getId());
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User updateNotificationPreference(Long userId, boolean emailNotifications) {
        User user = getUserById(userId);
        user.setEmailNotifications(emailNotifications);

        User updatedUser = userRepository.save(user);
        log.info("Updated notification preferences for user {}: {}", userId, emailNotifications);

        return updatedUser;
    }
}

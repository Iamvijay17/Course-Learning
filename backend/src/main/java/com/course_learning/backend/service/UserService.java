package com.course_learning.backend.service;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.course_learning.backend.config.JwtUtil;
import com.course_learning.backend.dto.AccountDeletionRequest;
import com.course_learning.backend.dto.ChangeEmailRequest;
import com.course_learning.backend.dto.ChangePasswordRequest;
import com.course_learning.backend.dto.ProfileUpdateRequest;
import com.course_learning.backend.dto.UserSettings;
import com.course_learning.backend.model.User;
import com.course_learning.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().stream().collect(Collectors.toList());
    }

    public User createUser(User userData) {
        User user = new User();

        user.setFirstName(userData.getFirstName());
        user.setLastName(userData.getLastName());
        user.setUserName(userData.getUserName());
        user.setEmail(userData.getEmail());
        user.setPassword(passwordEncoder.encode(userData.getPassword()));
        user.setRole(userData.getRole());
        user.setActive(true);
        user.setCreatedAt(userData.getCreatedAt());
        user.setUpdatedAt(userData.getUpdatedAt());

        User savedUser = userRepository.save(user);
        return savedUser;
    }

    public String login(String userName, String password) {
        User user = userRepository.findByUserName(userName);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtil.generateToken(userName, user.getUserId(), user.getRole(), user.getEmail(), user.getFirstName(), user.getLastName(), user.isActive(), user.isVerified());
        }
        return null;
    }

    public Boolean deleteUserById(String userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User getUserByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    // ========== PROFILE MANAGEMENT METHODS ==========

    public String getUserIdFromToken(String token) {
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    public User updateUserProfile(String userId, ProfileUpdateRequest request) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        // Note: Bio and website fields would need to be added to User model
        // For now, we'll skip them as they're not in the current User entity

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("USER_NOT_FOUND:User account not found");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("INVALID_CURRENT_PASSWORD:Current password is incorrect");
        }

        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("PASSWORD_MISMATCH:New password and confirmation do not match");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void changeEmail(String userId, ChangeEmailRequest request) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }

        // Check if email is already taken
        User existingUser = userRepository.findByEmail(request.getNewEmail());
        if (existingUser != null && !existingUser.getUserId().equals(userId)) {
            throw new RuntimeException("Email is already in use");
        }

        // Update email and mark as unverified
        user.setEmail(request.getNewEmail());
        user.setVerified(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // TODO: Send verification email to new email address
    }

    public String uploadProfilePicture(String userId, MultipartFile file) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Check file type (basic validation)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Invalid file type. Only images are allowed.");
        }

        // Check file size (5MB limit)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 5MB limit");
        }

        // TODO: Implement actual file upload to cloud storage (AWS S3, etc.)
        // For now, return a mock URL
        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
        String profilePictureUrl = "https://cdn.example.com/profiles/" + fileName;

        // TODO: Save profile picture URL to user entity (would need to add field to User model)
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return profilePictureUrl;
    }

    public void deleteProfilePicture(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // TODO: Delete file from cloud storage
        // TODO: Remove profile picture URL from user entity

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserSettings getUserSettings(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // TODO: Implement actual settings storage (database table or user preferences)
        // For now, return default settings
        return new UserSettings();
    }

    public void updateUserSettings(String userId, UserSettings settings) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // TODO: Save settings to database
        // For now, just update the user's timestamp
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void deleteUserAccount(String userId, AccountDeletionRequest request) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }

        // TODO: Implement soft delete or actual account deletion
        // For now, just mark as inactive
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // TODO: Schedule account deletion after grace period
        // TODO: Send confirmation email
        // TODO: Anonymize user data
    }

    public void verifyEmail(String userId, String verificationCode) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.isVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        // TODO: Implement proper verification code validation
        // For now, accept any 6-digit code
        if (verificationCode == null || !verificationCode.matches("\\d{6}")) {
            throw new RuntimeException("Invalid verification code");
        }

        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void resendEmailVerification(String userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.isVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        // TODO: Generate and send new verification code via email
        // For now, just log the action
        System.out.println("Verification email sent to: " + user.getEmail());
    }
}

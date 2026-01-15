package com.course_learning.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.course_learning.backend.config.JwtUtil;
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
            return jwtUtil.generateToken(userName);
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
}

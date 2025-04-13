package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final UserRepository userRepository;
    private User currentUser;

    public SessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isFaculty() {
        return isLoggedIn() && (currentUser.getRole() == User.Role.FACULTY);
    }

    public boolean isAdmin() {
        return isLoggedIn() && currentUser.getRole() == User.Role.ADMIN;
    }

    public boolean isStudent() {
        return isLoggedIn() && currentUser.getRole() == User.Role.STUDENT;
    }

    public void logout() {
        this.currentUser = null;
    }
}
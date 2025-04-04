package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;



    // Constructor injection instead of field injection
    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;

        // Initialize with a test user

    }

    public User getUserByUserEmail(String email) {

       return userRepo.getUserByEmail(email);
    }

    public User registerUser(User user) {
//        // Generate a unique user ID in a real application
//        if (user.getUserId() == null) {
//            user.setUserId(userDatabase.size() + 1);
//        }

        // Set default role if not provided TODO add default behavior of role in db
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save to database
        int userId=userRepo.save(user);
//        userDatabase.put(user.getEmail(), user);
        user.setUserId(userId);
        return user;
    }

    // For development purposes, set the encoded password for the test user
    public void setEncodedPasswordForTestUser(String email) {
        User user = userRepo.getUserByEmail(email);
        if (user != null && (user.getPassword() == null || user.getPassword().startsWith("{noop}"))) {
            String rawPassword = user.getPassword() != null ?
                    user.getPassword().replace("{noop}", "") : "1010";
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
    }


    public Optional<User> getUserById(int id) {
       return userRepo.getUserById(id);
    }
}
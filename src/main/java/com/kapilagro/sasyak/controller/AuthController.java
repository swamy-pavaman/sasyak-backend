package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.AuthResponse;
import com.kapilagro.sasyak.model.TokenRequest;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.services.UserService;
import com.kapilagro.sasyak.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    // Constructor injection instead of field injection
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            // Check if user already exists
            if (userService.getUserByUserEmail(user.getEmail()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
            }

            // Register the user
            User registeredUser = userService.registerUser(user);

            // Create UserDetails for token generation
            UserDetails userDetails = userDetailsService.loadUserByUsername(registeredUser.getEmail());

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Create response
            AuthResponse response = AuthResponse.builder()
                    .userId(registeredUser.getUserId())
                    .email(registeredUser.getEmail())
                    .name(registeredUser.getName())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during registration: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try {
            // Ensure the test user has an encoded password
            userService.setEncodedPasswordForTestUser(user.getEmail());

            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );
            System.out.println(user);

            // Get user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            User userFromDb = userService.getUserByUserEmail(user.getEmail());

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Create response
            AuthResponse response = AuthResponse.builder()
                    .userId(userFromDb.getUserId())
                    .email(userFromDb.getEmail())
                    .name(userFromDb.getName())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during login: " + e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRequest request) {
        try {
            // Extract username from refresh token
            String username = jwtUtil.extractUsernameFromRefreshToken(request.getRefreshToken());

            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtUtil.validateRefreshToken(request.getRefreshToken(), userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
            }

            // Generate new access token
            String accessToken = jwtUtil.generateAccessToken(userDetails);

            // Create response
            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(request.getRefreshToken()) // Return the same refresh token
                    .email(username)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }
}
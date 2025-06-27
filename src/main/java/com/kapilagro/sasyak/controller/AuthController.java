package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.AuthResponse;
import com.kapilagro.sasyak.model.TokenRequest;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.services.EmailService;
import com.kapilagro.sasyak.services.UserService;
import com.kapilagro.sasyak.utils.JwtUtil;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // Added for forgot password

    // Constructor injection
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            UserService userService,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService; // Injected
    }

    @PostMapping("/superadmin/login")
    public ResponseEntity<?> loginSuperAdmin(@RequestBody User user) {
        String email = user.getEmail();
        String rawPassword = user.getPassword();

        try {
            User userFromDb = userService.getSuperAdminByEmail(email);
            if (userFromDb == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authorized as super admin");
            }

            String encodedPassword = userFromDb.getPassword();
            boolean match = passwordEncoder.matches(rawPassword, encodedPassword);

            if (!match) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials (manual match failed)");
            }

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            AuthResponse response = AuthResponse.builder()
                    .userId(userFromDb.getUserId())
                    .email(userFromDb.getEmail())
                    .name(userFromDb.getName())
                    .role(userFromDb.getRole())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            System.out.println("✅ [DEBUG] Login successful for super admin: " + email);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            System.out.println("❌ [DEBUG] Spring Security authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        } catch (Exception e) {
            System.out.println("💥 [ERROR] Unexpected error during login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during login: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            if (userService.getUserByUserEmail(user.getEmail()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
            }

            User registeredUser = userService.registerUser(user);

            UserDetails userDetails = userDetailsService.loadUserByUsername(registeredUser.getEmail());

            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

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
            userService.setEncodedPasswordForTestUser(user.getEmail());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            User userFromDb = userService.getUserByUserEmail(user.getEmail());

            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            AuthResponse response = AuthResponse.builder()
                    .userId(userFromDb.getUserId())
                    .email(userFromDb.getEmail())
                    .name(userFromDb.getName())
                    .accessToken(accessToken)
                    .role(userFromDb.getRole())
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
            String username = jwtUtil.extractUsernameFromRefreshToken(request.getRefreshToken());

            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.validateRefreshToken(request.getRefreshToken(), userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
            }

            String accessToken = jwtUtil.generateAccessToken(userDetails);

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(request.getRefreshToken())
                    .email(username)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody PasswordResetRequest request) {
        try {
            // Validate email
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                System.out.println("❌ [DEBUG] Email is required");
                return ResponseEntity.badRequest().body("Email is required");
            }

            // Fetch user
            User user = userService.getUserByUserEmail(request.getEmail());
            if (user == null) {
                System.out.println("❌ [DEBUG] User not found for email: " + request.getEmail());
                return ResponseEntity.badRequest().body("Email not found");
            }

            // Generate reset token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("email", user.getEmail());
            String resetToken;
            try {
                resetToken = jwtUtil.generateResetToken(claims, jwtUtil.RESET_TOKEN_EXPIRY);
                System.out.println("✅ [DEBUG] Reset token generated for email: " + request.getEmail());
            } catch (Exception e) {
                System.out.println("💥 [ERROR] Failed to generate reset token for email: " + request.getEmail() + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate reset token: " + e.getMessage());
            }

            // Update user with reset token and expiry
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(new Date(System.currentTimeMillis() + jwtUtil.RESET_TOKEN_EXPIRY));
            try {
                userService.updateUser(user);
                System.out.println("✅ [DEBUG] User updated with reset token for email: " + request.getEmail());
            } catch (Exception e) {
                System.out.println("💥 [ERROR] Failed to update user with reset token for email: " + request.getEmail() + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update user: " + e.getMessage());
            }

            // Send reset email
            String resetLink = String.format("%s/reset-password?token=%s", "https://kapilagro.com", resetToken);
            try {
                emailService.sendResetEmail(user.getEmail(), user.getName(), resetLink);
                System.out.println("✅ [DEBUG] Reset link sent to email: " + request.getEmail());
            } catch (Exception e) {
                System.out.println("💥 [ERROR] Failed to send reset email to: " + request.getEmail() + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send reset email: " + e.getMessage());
            }

            return ResponseEntity.ok("Reset link sent to your email");
        } catch (Exception e) {
            System.out.println("💥 [ERROR] Unexpected error in forgotPassword for email: " + request.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process request: " + e.getMessage());
        }
    }

    // Reset Password Endpoint
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // Validate reset token
            String email = jwtUtil.extractUsernameFromResetToken(request.getToken());
            if (email == null || !jwtUtil.validateResetToken(request.getToken())) {
                return ResponseEntity.badRequest().body("Invalid or expired reset token");
            }

            User user = userService.getUserByUserEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Check token expiry
            if (user.getResetTokenExpiry() != null && user.getResetTokenExpiry().before(new Date())) {
                return ResponseEntity.badRequest().body("Reset token has expired");
            }

            // Verify new password matches confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body("Passwords do not match");
            }

            // Update password in database
            String encodedPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPassword(encodedPassword);
            user.setResetToken(null); // Clear reset token
            user.setResetTokenExpiry(null); // Clear expiry
            userService.updateUser(user); // Save updated user

            // Cross-check the new password (optional validation)
            if (!passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Password update validation failed");
            }

            return ResponseEntity.ok("Password reset successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reset password: " + e.getMessage());
        }
    }

    @Data
    static class PasswordResetRequest {
        private String email;
    }

    @Data
    static class ResetPasswordRequest {
        private String token;
        private String newPassword;
        private String confirmPassword;
    }
}
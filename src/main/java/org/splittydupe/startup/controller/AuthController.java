package org.splittydupe.startup.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.*;
import org.splittydupe.startup.model.User;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword(), request.getDisplayName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    AuthResponse.builder()
                            .message("User registered successfully. Please check your email to verify your account.")
                            .email(user.getEmail())
                            .userId(user.getUid())
                            .emailVerified(false)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Signup failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ErrorResponse.builder()
                            .error("Signup failed")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Optional<User> userOpt = userService.authenticateUser(request.getEmail(), request.getPassword());

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ErrorResponse.builder()
                                .error("Authentication failed")
                                .message("Invalid email or password")
                                .build()
                );
            }

            User user = userOpt.get();
            String token = jwtService.generateToken(user.getEmail());

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .token(token)
                            .email(user.getEmail())
                            .userId(user.getUid())
                            .emailVerified(user.isEmailVerified())
                            .message("Login successful")
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ErrorResponse.builder()
                            .error("Authentication failed")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            boolean verified = userService.verifyEmail(token);

            if (verified) {
                return ResponseEntity.ok(
                        ErrorResponse.builder()
                                .message("Email verified successfully! You can now log in.")
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        ErrorResponse.builder()
                                .error("Verification failed")
                                .message("Invalid or expired verification token")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Email verification failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .error("Verification failed")
                            .message("An error occurred during verification")
                            .build()
            );
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.initiatePasswordReset(request.getEmail());

            // Always return success to prevent email enumeration
            return ResponseEntity.ok(
                    ErrorResponse.builder()
                            .message("If an account with that email exists, a password reset link has been sent.")
                            .build()
            );
        } catch (Exception e) {
            log.error("Password reset request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .error("Request failed")
                            .message("An error occurred processing your request")
                            .build()
            );
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            boolean reset = userService.resetPassword(request.getToken(), request.getNewPassword());

            if (reset) {
                return ResponseEntity.ok(
                        ErrorResponse.builder()
                                .message("Password reset successfully. You can now log in with your new password.")
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        ErrorResponse.builder()
                                .error("Reset failed")
                                .message("Invalid or expired reset token")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Password reset failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .error("Reset failed")
                            .message("An error occurred during password reset")
                            .build()
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer "
            String email = jwtService.extractUsername(token);

            Optional<User> userOpt = userService.getUserByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ErrorResponse.builder()
                                .error("User not found")
                                .message("User not found")
                                .build()
                );
            }

            User user = userOpt.get();
            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .email(user.getEmail())
                            .userId(user.getUid())
                            .emailVerified(user.isEmailVerified())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .error("Request failed")
                            .message("Failed to get user information")
                            .build()
            );
        }
    }
}

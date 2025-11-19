package org.splittydupe.startup.service;

import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.User;
import org.splittydupe.startup.model.VerificationToken;
import org.splittydupe.startup.repository.UserRepository;
import org.splittydupe.startup.repository.VerificationTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

    public User registerUser(String email, String password) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = User.builder()
                .uid(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .emailVerified(false)
                .enabled(true)
                .roles(new ArrayList<>())
                .createdAt(Timestamp.now())
                .build();

        user.getRoles().add("ROLE_USER");

        userRepository.save(user);
        log.info("User registered successfully: {}", email);

        String token = createVerificationToken(user, "email_verification");
        emailService.sendVerificationEmail(email, token);

        return user;
    }

    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: User not found - {}", email);
            return Optional.empty();
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Authentication failed: Invalid password - {}", email);
            return Optional.empty();
        }

        if (!user.isEmailVerified()) {
            log.warn("Authentication failed: Email not verified - {}", email);
            throw new RuntimeException("Email not verified. Please verify your email before logging in.");
        }

        if (!user.isEnabled()) {
            log.warn("Authentication failed: Account disabled - {}", email);
            throw new RuntimeException("Account is disabled");
        }

        user.setLastLoginAt(Timestamp.now());
        userRepository.update(user);

        log.info("User authenticated successfully: {}", email);
        return Optional.of(user);
    }

    public String createVerificationToken(User user, String tokenType) {
        String tokenValue = UUID.randomUUID().toString();

        Timestamp now = Timestamp.now();
        Timestamp expiry = tokenType.equals("password_reset")
                ? Timestamp.ofTimeSecondsAndNanos(
                    now.getSeconds() + TimeUnit.HOURS.toSeconds(1),  // 1 hour for password reset
                    now.getNanos())
                : Timestamp.ofTimeSecondsAndNanos(
                    now.getSeconds() + TimeUnit.HOURS.toSeconds(24), // 24 hours for email verification
                    now.getNanos());

        VerificationToken token = VerificationToken.builder()
                .uid(UUID.randomUUID().toString())
                .token(tokenValue)
                .userEmail(user.getEmail())
                .userId(user.getUid())
                .tokenType(tokenType)
                .expiryDate(expiry)
                .used(false)
                .createdAt(now)
                .build();

        tokenRepository.save(token);
        log.info("Verification token created for user: {}, type: {}", user.getEmail(), tokenType);

        return tokenValue;
    }

    public boolean verifyEmail(String tokenValue) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            log.warn("Verification failed: Token not found");
            return false;
        }

        VerificationToken token = tokenOpt.get();

        if (token.isUsed()) {
            log.warn("Verification failed: Token already used");
            return false;
        }

        if (token.getExpiryDate().compareTo(Timestamp.now()) < 0) {
            log.warn("Verification failed: Token expired");
            return false;
        }

        if (!"email_verification".equals(token.getTokenType())) {
            log.warn("Verification failed: Invalid token type");
            return false;
        }

        token.setUsed(true);
        tokenRepository.update(token);

        Optional<User> userOpt = userRepository.findById(token.getUserId());
        if (userOpt.isEmpty()) {
            log.error("Verification failed: User not found for token");
            return false;
        }

        User user = userOpt.get();
        user.setEmailVerified(true);
        userRepository.update(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getEmail().split("@")[0]);

        log.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }

    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        String token = createVerificationToken(user, "password_reset");
        emailService.sendPasswordResetEmail(email, token);

        log.info("Password reset email sent to: {}", email);
    }

    public boolean resetPassword(String tokenValue, String newPassword) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            log.warn("Password reset failed: Token not found");
            return false;
        }

        VerificationToken token = tokenOpt.get();

        if (token.isUsed()) {
            log.warn("Password reset failed: Token already used");
            return false;
        }

        if (token.getExpiryDate().compareTo(Timestamp.now()) < 0) {
            log.warn("Password reset failed: Token expired");
            return false;
        }

        if (!"password_reset".equals(token.getTokenType())) {
            log.warn("Password reset failed: Invalid token type");
            return false;
        }

        token.setUsed(true);
        tokenRepository.update(token);

        Optional<User> userOpt = userRepository.findById(token.getUserId());
        if (userOpt.isEmpty()) {
            log.error("Password reset failed: User not found for token");
            return false;
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.update(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
        return true;
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(String uid) {
        return userRepository.findById(uid);
    }
}

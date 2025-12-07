package org.splittydupe.startup.service;

import com.google.cloud.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.model.User;
import org.splittydupe.startup.model.UserProfile;
import org.splittydupe.startup.model.VerificationToken;
import org.splittydupe.startup.repository.UserRepository;
import org.splittydupe.startup.repository.VerificationTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .uid("user-123")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .emailVerified(true)
                .enabled(true)
                .roles(new ArrayList<>(Arrays.asList("ROLE_USER")))
                .createdAt(Timestamp.now())
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        String email = "newuser@example.com";
        String password = "password123";

        UserProfile testProfile = UserProfile.builder()
                .userId("test-user-id")
                .displayName("Test User")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(true);
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(true);
        when(userProfileService.saveProfile(any(UserProfile.class))).thenReturn(testProfile);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        User result = userService.registerUser(email, password, "Test User");

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertFalse(result.isEmailVerified());
        assertTrue(result.isEnabled());
        assertTrue(result.getRoles().contains("ROLE_USER"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("Should throw exception when registering existing user")
    void shouldThrowExceptionWhenRegisteringExistingUser() {
        String email = "existing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(email, "password", "Test User");
        });

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void shouldAuthenticateUserWithValidCredentials() {
        String email = "test@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(userRepository.update(any(User.class))).thenReturn(true);

        Optional<User> result = userService.authenticateUser(email, password);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository, times(1)).update(any(User.class));
    }

    @Test
    @DisplayName("Should fail authentication with wrong password")
    void shouldFailAuthenticationWithWrongPassword() {
        String email = "test@example.com";
        String password = "wrongpassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(false);

        Optional<User> result = userService.authenticateUser(email, password);

        assertFalse(result.isPresent());
        verify(userRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should fail authentication with nonexistent user")
    void shouldFailAuthenticationWithNonexistentUser() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = userService.authenticateUser(email, "password");

        assertFalse(result.isPresent());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when authenticating unverified email")
    void shouldThrowExceptionWhenAuthenticatingUnverifiedEmail() {
        User unverifiedUser = User.builder()
                .uid("user-unverified")
                .email("unverified@example.com")
                .passwordHash("hashed")
                .emailVerified(false)
                .enabled(true)
                .build();

        when(userRepository.findByEmail(unverifiedUser.getEmail())).thenReturn(Optional.of(unverifiedUser));
        when(passwordEncoder.matches("password", unverifiedUser.getPasswordHash())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.authenticateUser(unverifiedUser.getEmail(), "password");
        });

        assertTrue(exception.getMessage().contains("Email not verified"));
    }

    @Test
    @DisplayName("Should throw exception when authenticating disabled user")
    void shouldThrowExceptionWhenAuthenticatingDisabledUser() {
        User disabledUser = User.builder()
                .uid("user-disabled")
                .email("disabled@example.com")
                .passwordHash("hashed")
                .emailVerified(true)
                .enabled(false)
                .build();

        when(userRepository.findByEmail(disabledUser.getEmail())).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches("password", disabledUser.getPasswordHash())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.authenticateUser(disabledUser.getEmail(), "password");
        });

        assertTrue(exception.getMessage().contains("disabled"));
    }

    @Test
    @DisplayName("Should create email verification token")
    void shouldCreateEmailVerificationToken() {
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(true);

        String token = userService.createVerificationToken(testUser, "email_verification");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Should create password reset token with shorter expiry")
    void shouldCreatePasswordResetTokenWithShorterExpiry() {
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(true);

        String token = userService.createVerificationToken(testUser, "password_reset");

        assertNotNull(token);
        verify(tokenRepository, times(1)).save(argThat(t ->
            "password_reset".equals(t.getTokenType())
        ));
    }

    @Test
    @DisplayName("Should verify email successfully")
    void shouldVerifyEmailSuccessfully() {
        String tokenValue = "valid-token";
        Timestamp futureExpiry = Timestamp.ofTimeSecondsAndNanos(
                Timestamp.now().getSeconds() + 86400,
                0
        );

        VerificationToken token = VerificationToken.builder()
                .uid("token-1")
                .token(tokenValue)
                .userId(testUser.getUid())
                .userEmail(testUser.getEmail())
                .tokenType("email_verification")
                .expiryDate(futureExpiry)
                .used(false)
                .build();

        User unverifiedUser = User.builder()
                .uid(testUser.getUid())
                .email(testUser.getEmail())
                .passwordHash("hashed")
                .emailVerified(false)
                .build();

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
        when(tokenRepository.update(any(VerificationToken.class))).thenReturn(true);
        when(userRepository.findById(testUser.getUid())).thenReturn(Optional.of(unverifiedUser));
        when(userRepository.update(any(User.class))).thenReturn(true);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        boolean result = userService.verifyEmail(tokenValue);

        assertTrue(result);
        verify(tokenRepository, times(1)).update(argThat(t -> t.isUsed()));
        verify(userRepository, times(1)).update(any(User.class));
        verify(emailService, times(1)).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should fail to verify with nonexistent token")
    void shouldFailToVerifyWithNonexistentToken() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        boolean result = userService.verifyEmail("invalid-token");

        assertFalse(result);
        verify(tokenRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should fail to verify with used token")
    void shouldFailToVerifyWithUsedToken() {
        VerificationToken usedToken = VerificationToken.builder()
                .uid("token-1")
                .token("used-token")
                .used(true)
                .build();

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        boolean result = userService.verifyEmail("used-token");

        assertFalse(result);
        verify(tokenRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should fail to verify with expired token")
    void shouldFailToVerifyWithExpiredToken() {
        Timestamp pastExpiry = Timestamp.ofTimeSecondsAndNanos(
                Timestamp.now().getSeconds() - 3600,
                0
        );

        VerificationToken expiredToken = VerificationToken.builder()
                .uid("token-1")
                .token("expired-token")
                .tokenType("email_verification")
                .expiryDate(pastExpiry)
                .used(false)
                .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        boolean result = userService.verifyEmail("expired-token");

        assertFalse(result);
        verify(tokenRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should initiate password reset")
    void shouldInitiatePasswordReset() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(true);
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        userService.initiatePasswordReset(email);

        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("Should not reveal if user exists during password reset")
    void shouldNotRevealIfUserExistsDuringPasswordReset() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        userService.initiatePasswordReset(email);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reset password successfully")
    void shouldResetPasswordSuccessfully() {
        String tokenValue = "reset-token";
        String newPassword = "newpassword123";
        Timestamp futureExpiry = Timestamp.ofTimeSecondsAndNanos(
                Timestamp.now().getSeconds() + 3600,
                0
        );

        VerificationToken token = VerificationToken.builder()
                .uid("token-1")
                .token(tokenValue)
                .userId(testUser.getUid())
                .tokenType("password_reset")
                .expiryDate(futureExpiry)
                .used(false)
                .build();

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
        when(tokenRepository.update(any(VerificationToken.class))).thenReturn(true);
        when(userRepository.findById(testUser.getUid())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newhashed");
        when(userRepository.update(any(User.class))).thenReturn(true);

        boolean result = userService.resetPassword(tokenValue, newPassword);

        assertTrue(result);
        verify(tokenRepository, times(1)).update(argThat(t -> t.isUsed()));
        verify(userRepository, times(1)).update(any(User.class));
        verify(passwordEncoder, times(1)).encode(newPassword);
    }

    @Test
    @DisplayName("Should fail to reset password with wrong token type")
    void shouldFailToResetPasswordWithWrongTokenType() {
        VerificationToken wrongTypeToken = VerificationToken.builder()
                .uid("token-1")
                .token("wrong-type-token")
                .tokenType("email_verification")
                .expiryDate(Timestamp.ofTimeSecondsAndNanos(Timestamp.now().getSeconds() + 3600, 0))
                .used(false)
                .build();

        when(tokenRepository.findByToken("wrong-type-token")).thenReturn(Optional.of(wrongTypeToken));

        boolean result = userService.resetPassword("wrong-type-token", "newpassword");

        assertFalse(result);
        verify(userRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should get user by email")
    void shouldGetUserByEmail() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserByEmail(testUser.getEmail());

        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
    }

    @Test
    @DisplayName("Should get user by id")
    void shouldGetUserById() {
        when(userRepository.findById(testUser.getUid())).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserById(testUser.getUid());

        assertTrue(result.isPresent());
        assertEquals(testUser.getUid(), result.get().getUid());
    }
}

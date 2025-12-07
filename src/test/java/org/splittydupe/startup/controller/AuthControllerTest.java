package org.splittydupe.startup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.splittydupe.startup.TestConfig;
import org.splittydupe.startup.dto.*;
import org.splittydupe.startup.model.User;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.UserService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    private User testUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = TestConfig.createTestUser();

        signupRequest = new SignupRequest();
        signupRequest.setEmail(TestConfig.TEST_USER_EMAIL);
        signupRequest.setPassword("password123");
        signupRequest.setDisplayName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail(TestConfig.TEST_USER_EMAIL);
        loginRequest.setPassword("password123");
    }

    @Test
    @WithMockUser
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        when(userService.registerUser(anyString(), anyString(), anyString())).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").value(testUser.getUid()))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).registerUser(signupRequest.getEmail(), signupRequest.getPassword(), signupRequest.getDisplayName());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when signup fails")
    void shouldReturn400WhenSignupFails() throws Exception {
        when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Email already exists"));

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Signup failed"))
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).registerUser(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should login successfully")
    void shouldLoginSuccessfully() throws Exception {
        String token = "test.jwt.token";
        when(userService.authenticateUser(anyString(), anyString())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(anyString())).thenReturn(token);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").value(testUser.getUid()))
                .andExpect(jsonPath("$.emailVerified").value(testUser.isEmailVerified()))
                .andExpect(jsonPath("$.message").value("Login successful"));

        verify(userService, times(1)).authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
        verify(jwtService, times(1)).generateToken(testUser.getEmail());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 401 when login fails with invalid credentials")
    void shouldReturn401WhenLoginFailsInvalidCredentials() throws Exception {
        when(userService.authenticateUser(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication failed"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService, times(1)).authenticateUser(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 401 when login throws exception")
    void shouldReturn401WhenLoginThrowsException() throws Exception {
        when(userService.authenticateUser(anyString(), anyString()))
                .thenThrow(new RuntimeException("Authentication error"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication failed"));

        verify(userService, times(1)).authenticateUser(anyString(), anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should verify email successfully")
    void shouldVerifyEmailSuccessfully() throws Exception {
        String verificationToken = "valid-token";
        when(userService.verifyEmail(verificationToken)).thenReturn(true);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", verificationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully! You can now log in."));

        verify(userService, times(1)).verifyEmail(verificationToken);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 for invalid email verification token")
    void shouldReturn400ForInvalidEmailVerificationToken() throws Exception {
        String invalidToken = "invalid-token";
        when(userService.verifyEmail(invalidToken)).thenReturn(false);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", invalidToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Verification failed"))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));

        verify(userService, times(1)).verifyEmail(invalidToken);
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle forgot password request")
    void shouldHandleForgotPasswordRequest() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(TestConfig.TEST_USER_EMAIL);

        doNothing().when(userService).initiatePasswordReset(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).initiatePasswordReset(request.getEmail());
    }

    @Test
    @WithMockUser
    @DisplayName("Should reset password successfully")
    void shouldResetPasswordSuccessfully() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken("reset-token");
        request.setNewPassword("newPassword123");

        when(userService.resetPassword(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully. You can now log in with your new password."));

        verify(userService, times(1)).resetPassword(request.getToken(), request.getNewPassword());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 for invalid password reset token")
    void shouldReturn400ForInvalidPasswordResetToken() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");

        when(userService.resetPassword(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Reset failed"))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));

        verify(userService, times(1)).resetPassword(anyString(), anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should get current user info")
    void shouldGetCurrentUserInfo() throws Exception {
        String token = "Bearer test.jwt.token";
        when(jwtService.extractUsername(anyString())).thenReturn(testUser.getEmail());
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").value(testUser.getUid()))
                .andExpect(jsonPath("$.emailVerified").value(testUser.isEmailVerified()));

        verify(jwtService, atLeastOnce()).extractUsername("test.jwt.token");
        verify(userService, times(1)).getUserByEmail(testUser.getEmail());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when user not found in /me endpoint")
    void shouldReturn404WhenUserNotFoundInMeEndpoint() throws Exception {
        String token = "Bearer test.jwt.token";
        when(jwtService.extractUsername(anyString())).thenReturn(testUser.getEmail());
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(jwtService, atLeastOnce()).extractUsername(anyString());
        verify(userService, times(1)).getUserByEmail(anyString());
    }
}

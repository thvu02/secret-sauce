package org.splittydupe.startup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.exception.EmailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@splittydupe.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("Should send verification email successfully")
    void shouldSendVerificationEmailSuccessfully() {
        String toEmail = "test@example.com";
        String token = "verification-token-123";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendVerificationEmail(toEmail, token);

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("noreply@splittydupe.com", sentMessage.getFrom());
        assertArrayEquals(new String[]{toEmail}, sentMessage.getTo());
        assertEquals("SplittyDupe - Verify Your Email Address", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(token));
        assertTrue(sentMessage.getText().contains("http://localhost:5173/verify-email?token=" + token));
    }

    @Test
    @DisplayName("Should throw exception when verification email fails")
    void shouldThrowExceptionWhenVerificationEmailFails() {
        String toEmail = "test@example.com";
        String token = "token-123";
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailException exception = assertThrows(EmailException.class, () -> {
            emailService.sendVerificationEmail(toEmail, token);
        });

        assertTrue(exception.getMessage().contains("Failed to send verification email"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void shouldSendPasswordResetEmailSuccessfully() {
        String toEmail = "reset@example.com";
        String token = "reset-token-456";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendPasswordResetEmail(toEmail, token);

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("noreply@splittydupe.com", sentMessage.getFrom());
        assertArrayEquals(new String[]{toEmail}, sentMessage.getTo());
        assertEquals("SplittyDupe - Password Reset Request", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(token));
        assertTrue(sentMessage.getText().contains("http://localhost:5173/reset-password?token=" + token));
        assertTrue(sentMessage.getText().contains("1 hour"));
    }

    @Test
    @DisplayName("Should throw exception when password reset email fails")
    void shouldThrowExceptionWhenPasswordResetEmailFails() {
        String toEmail = "reset@example.com";
        String token = "token-456";
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailException exception = assertThrows(EmailException.class, () -> {
            emailService.sendPasswordResetEmail(toEmail, token);
        });

        assertTrue(exception.getMessage().contains("Failed to send password reset email"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void shouldSendWelcomeEmailSuccessfully() {
        String toEmail = "welcome@example.com";
        String userName = "John";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendWelcomeEmail(toEmail, userName);

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("noreply@splittydupe.com", sentMessage.getFrom());
        assertArrayEquals(new String[]{toEmail}, sentMessage.getTo());
        assertEquals("Welcome to SplittyDupe!", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains(userName));
        assertTrue(sentMessage.getText().contains("verified successfully"));
    }

    @Test
    @DisplayName("Should not throw exception when welcome email fails")
    void shouldNotThrowExceptionWhenWelcomeEmailFails() {
        String toEmail = "welcome@example.com";
        String userName = "John";
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail(toEmail, userName);
        });

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should include correct verification link format")
    void shouldIncludeCorrectVerificationLinkFormat() {
        String toEmail = "test@example.com";
        String token = "abc123";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendVerificationEmail(toEmail, token);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("verify-email?token=abc123"));
    }

    @Test
    @DisplayName("Should include correct reset link format")
    void shouldIncludeCorrectResetLinkFormat() {
        String toEmail = "test@example.com";
        String token = "xyz789";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendPasswordResetEmail(toEmail, token);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("reset-password?token=xyz789"));
    }

    @Test
    @DisplayName("Should use custom base URL when configured")
    void shouldUseCustomBaseUrlWhenConfigured() {
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://custom.domain.com");
        String toEmail = "test@example.com";
        String token = "token123";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendVerificationEmail(toEmail, token);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("https://custom.domain.com/verify-email?token=token123"));
    }
}

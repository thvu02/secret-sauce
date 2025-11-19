package org.splittydupe.startup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.exception.EmailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SplittyDupe - Verify Your Email Address");

            String verificationLink = baseUrl + "/verify-email?token=" + token;
            String emailBody = String.format(
                    "Welcome to SplittyDupe!\n\n" +
                    "Please verify your email address by clicking the link below:\n\n" +
                    "%s\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not create an account, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "The SplittyDupe Team",
                    verificationLink
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new EmailException("Failed to send verification email to: " + toEmail, e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SplittyDupe - Password Reset Request");

            String resetLink = baseUrl + "/reset-password?token=" + token;
            String emailBody = String.format(
                    "Hello,\n\n" +
                    "We received a request to reset your password for your SplittyDupe account.\n\n" +
                    "Click the link below to reset your password:\n\n" +
                    "%s\n\n" +
                    "This link will expire in 1 hour.\n\n" +
                    "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n" +
                    "Best regards,\n" +
                    "The SplittyDupe Team",
                    resetLink
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailException("Failed to send password reset email to: " + toEmail, e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to SplittyDupe!");

            String emailBody = String.format(
                    "Hello %s,\n\n" +
                    "Your email has been verified successfully!\n\n" +
                    "You can now enjoy all features of SplittyDupe:\n" +
                    "- Upload and parse receipts with OCR\n" +
                    "- Split expenses with friends\n" +
                    "- Track payment status\n" +
                    "- Generate PDF reports\n" +
                    "- Access your receipt history from any device\n\n" +
                    "Get started by logging in at: %s\n\n" +
                    "Best regards,\n" +
                    "The SplittyDupe Team",
                    userName,
                    baseUrl
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }
}

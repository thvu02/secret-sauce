package org.splittydupe.startup.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("Should create BCryptPasswordEncoder bean")
    void shouldCreateBCryptPasswordEncoderBean() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
    }

    @Test
    @DisplayName("Should encode passwords consistently")
    void shouldEncodePasswordsConsistently() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String rawPassword = "testPassword123";

        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded1);
        assertNotNull(encoded2);
        assertNotEquals(encoded1, encoded2); // BCrypt generates different salts
        assertTrue(passwordEncoder.matches(rawPassword, encoded1));
        assertTrue(passwordEncoder.matches(rawPassword, encoded2));
    }

    @Test
    @DisplayName("Should validate password matches")
    void shouldValidatePasswordMatches() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecretPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        assertTrue(matches);
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void shouldRejectIncorrectPassword() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String rawPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        assertFalse(matches);
    }

    @Test
    @DisplayName("Should handle empty password")
    void shouldHandleEmptyPassword() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String emptyPassword = "";

        String encoded = passwordEncoder.encode(emptyPassword);

        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(emptyPassword, encoded));
    }

    @Test
    @DisplayName("Should handle long passwords")
    void shouldHandleLongPasswords() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String longPassword = "a".repeat(100);

        String encoded = passwordEncoder.encode(longPassword);

        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(longPassword, encoded));
    }

    @Test
    @DisplayName("Should handle special characters in passwords")
    void shouldHandleSpecialCharactersInPasswords() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String specialPassword = "p@ssw0rd!#$%^&*()";

        String encoded = passwordEncoder.encode(specialPassword);

        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(specialPassword, encoded));
    }

    @Test
    @DisplayName("Should create different hashes for same password")
    void shouldCreateDifferentHashesForSamePassword() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "samePassword";

        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);
        String hash3 = passwordEncoder.encode(password);

        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
        assertNotEquals(hash1, hash3);
        assertTrue(passwordEncoder.matches(password, hash1));
        assertTrue(passwordEncoder.matches(password, hash2));
        assertTrue(passwordEncoder.matches(password, hash3));
    }
}

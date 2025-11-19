package org.splittydupe.startup.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.service.JwtService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should pass through when no Authorization header present")
    void shouldPassThroughWhenNoAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Should pass through when Authorization header doesn't start with Bearer")
    void shouldPassThroughWhenAuthorizationHeaderDoesntStartWithBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic some-token");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Should extract token from Authorization header")
    void shouldExtractTokenFromAuthorizationHeader() throws ServletException, IOException {
        String token = "valid-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(jwtService.isTokenValid(eq(token), anyString())).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, times(1)).extractUsername(token);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle invalid token gracefully")
    void shouldHandleInvalidTokenGracefully() throws ServletException, IOException {
        String invalidToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtService.extractUsername(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context before processing")
    void shouldClearSecurityContextBeforeProcessing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle empty Bearer token")
    void shouldHandleEmptyBearerToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should trim token from Bearer prefix")
    void shouldTrimTokenFromBearerPrefix() throws ServletException, IOException {
        String token = "token-without-spaces";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(jwtService.isTokenValid(eq(token), anyString())).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, times(1)).extractUsername(token);
    }
}

package org.splittydupe.startup.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.splittydupe.startup.TestConfig;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.ReceiptService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserDashboardController.class)
@DisplayName("User Dashboard Controller Tests")
class UserDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private JwtService jwtService;

    private Receipt testReceipt;
    private String authHeader;

    @BeforeEach
    void setUp() {
        testReceipt = TestConfig.createTestReceipt();
        testReceipt.setUserId(TestConfig.TEST_USER_EMAIL);
        authHeader = "Bearer test.jwt.token";
    }

    @Test
    @WithMockUser
    @DisplayName("Should get all user receipts successfully")
    void shouldGetAllUserReceiptsSuccessfully() throws Exception {
        List<Receipt> receipts = Arrays.asList(testReceipt, TestConfig.createMinimalReceipt());
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getUserReceipts(TestConfig.TEST_USER_EMAIL)).thenReturn(receipts);

        mockMvc.perform(get("/api/dashboard/receipts")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(jwtService, atLeastOnce()).extractUsername("test.jwt.token");
        verify(receiptService, times(1)).getUserReceipts(TestConfig.TEST_USER_EMAIL);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty list when user has no receipts")
    void shouldReturnEmptyListWhenUserHasNoReceipts() throws Exception {
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getUserReceipts(TestConfig.TEST_USER_EMAIL)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/receipts")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(receiptService, times(1)).getUserReceipts(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should get specific receipt by ID successfully")
    void shouldGetSpecificReceiptByIdSuccessfully() throws Exception {
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getReceipt(testReceipt.getUid())).thenReturn(testReceipt);

        mockMvc.perform(get("/api/dashboard/receipts/" + testReceipt.getUid())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(testReceipt.getUid()))
                .andExpect(jsonPath("$.vendor").value(testReceipt.getVendor()));

        verify(receiptService, times(1)).getReceipt(testReceipt.getUid());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when receipt not found")
    void shouldReturn404WhenReceiptNotFound() throws Exception {
        String nonExistentId = "non-existent-id";
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getReceipt(nonExistentId)).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/receipts/" + nonExistentId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Receipt not found"));

        verify(receiptService, times(1)).getReceipt(nonExistentId);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 403 when user tries to access another user's receipt")
    void shouldReturn403WhenUserTriesToAccessAnotherUsersReceipt() throws Exception {
        Receipt otherUserReceipt = TestConfig.createTestReceipt();
        otherUserReceipt.setUserId("other@example.com");

        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getReceipt(otherUserReceipt.getUid())).thenReturn(otherUserReceipt);

        mockMvc.perform(get("/api/dashboard/receipts/" + otherUserReceipt.getUid())
                        .header("Authorization", authHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"))
                .andExpect(jsonPath("$.message").value("You do not have permission to view this receipt"));

        verify(receiptService, times(1)).getReceipt(otherUserReceipt.getUid());
    }

    @Test
    @WithMockUser
    @DisplayName("Should delete receipt successfully")
    void shouldDeleteReceiptSuccessfully() throws Exception {
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getReceipt(testReceipt.getUid())).thenReturn(testReceipt);
        when(receiptService.deleteReceipt(testReceipt.getUid())).thenReturn(true);

        mockMvc.perform(delete("/api/dashboard/receipts/" + testReceipt.getUid())
                        .with(csrf())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true))
                .andExpect(jsonPath("$.message").value("Receipt deleted successfully"));

        verify(receiptService, times(1)).getReceipt(testReceipt.getUid());
        verify(receiptService, times(1)).deleteReceipt(testReceipt.getUid());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when trying to delete non-existent receipt")
    void shouldReturn404WhenTryingToDeleteNonExistentReceipt() throws Exception {
        String nonExistentId = "non-existent-id";
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getReceipt(nonExistentId)).thenReturn(null);

        mockMvc.perform(delete("/api/dashboard/receipts/" + nonExistentId)
                        .with(csrf())
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Receipt not found"));

        verify(receiptService, times(1)).getReceipt(nonExistentId);
        verify(receiptService, never()).deleteReceipt(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 403 when trying to delete another user's receipt")
    void shouldReturn403WhenTryingToDeleteAnotherUsersReceipt() throws Exception {
        Receipt otherUserReceipt = TestConfig.createTestReceipt();
        otherUserReceipt.setUserId("other@example.com");

        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.getReceipt(otherUserReceipt.getUid())).thenReturn(otherUserReceipt);

        mockMvc.perform(delete("/api/dashboard/receipts/" + otherUserReceipt.getUid())
                        .with(csrf())
                        .header("Authorization", authHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));

        verify(receiptService, times(1)).getReceipt(otherUserReceipt.getUid());
        verify(receiptService, never()).deleteReceipt(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/dashboard/receipts")
                        .header("Authorization", authHeader))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to retrieve receipts"));

        verify(jwtService, atLeastOnce()).extractUsername(anyString());
    }
}

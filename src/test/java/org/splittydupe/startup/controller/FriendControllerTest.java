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
import org.splittydupe.startup.model.Friend;
import org.splittydupe.startup.service.FriendService;
import org.splittydupe.startup.service.JwtService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendController.class)
@DisplayName("Friend Controller Tests")
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FriendService friendService;

    @MockBean
    private JwtService jwtService;

    private Friend testFriend;

    @BeforeEach
    void setUp() {
        testFriend = Friend.builder()
                .id("friend-123")
                .userId("user-123")
                .displayName("John Doe")
                .build();
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should get all friends successfully")
    void shouldGetAllFriendsSuccessfully() throws Exception {
        when(friendService.getFriendsByUserId("user-123"))
                .thenReturn(Arrays.asList(testFriend));

        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].displayName").value("John Doe"));

        verify(friendService, times(1)).getFriendsByUserId("user-123");
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should return empty list when no friends")
    void shouldReturnEmptyListWhenNoFriends() throws Exception {
        when(friendService.getFriendsByUserId("user-123"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should get friend by ID successfully")
    void shouldGetFriendByIdSuccessfully() throws Exception {
        when(friendService.getFriendById("friend-123"))
                .thenReturn(Optional.of(testFriend));

        mockMvc.perform(get("/api/friends/friend-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("friend-123"))
                .andExpect(jsonPath("$.displayName").value("John Doe"));

        verify(friendService, times(1)).getFriendById("friend-123");
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should return 404 when friend not found")
    void shouldReturn404WhenFriendNotFound() throws Exception {
        when(friendService.getFriendById("non-existent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/friends/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Friend not found"));

        verify(friendService, times(1)).getFriendById("non-existent");
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should create friend successfully")
    void shouldCreateFriendSuccessfully() throws Exception {
        Friend newFriend = Friend.builder()
                .displayName("Jane Doe")
                .build();

        Friend savedFriend = Friend.builder()
                .id("friend-456")
                .userId("user-123")
                .displayName("Jane Doe")
                .build();

        when(friendService.createFriend(any(Friend.class))).thenReturn(savedFriend);

        mockMvc.perform(post("/api/friends")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newFriend)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("friend-456"))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.displayName").value("Jane Doe"));

        verify(friendService, times(1)).createFriend(any(Friend.class));
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should return 400 when creating friend with invalid data")
    void shouldReturn400WhenCreatingFriendWithInvalidData() throws Exception {
        Friend invalidFriend = Friend.builder().build();

        when(friendService.createFriend(any(Friend.class)))
                .thenThrow(new IllegalArgumentException("Name is required"));

        mockMvc.perform(post("/api/friends")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFriend)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name is required"));
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should update friend successfully")
    void shouldUpdateFriendSuccessfully() throws Exception {
        Friend updatedFriend = Friend.builder()
                .id("friend-123")
                .displayName("John Updated")
                .build();

        when(friendService.updateFriend(eq("friend-123"), any(Friend.class)))
                .thenReturn(updatedFriend);

        mockMvc.perform(put("/api/friends/friend-123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedFriend)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("John Updated"));

        verify(friendService, times(1)).updateFriend(eq("friend-123"), any(Friend.class));
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should delete friend successfully")
    void shouldDeleteFriendSuccessfully() throws Exception {
        doNothing().when(friendService).deleteFriend("friend-123");

        mockMvc.perform(delete("/api/friends/friend-123")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(friendService, times(1)).deleteFriend("friend-123");
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        when(friendService.getFriendsByUserId(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }
}

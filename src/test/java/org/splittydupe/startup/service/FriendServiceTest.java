package org.splittydupe.startup.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.model.Friend;
import org.splittydupe.startup.model.UserProfile;
import org.splittydupe.startup.repository.FriendRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendService Tests")
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private FriendService friendService;

    @Test
    @DisplayName("Should create friend successfully")
    void shouldCreateFriendSuccessfully() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        when(friendRepository.save(friend)).thenReturn(friend);
        when(friendRepository.findByUserId("user-1")).thenReturn(Arrays.asList());
        when(userProfileService.getProfileByUserId("user-1")).thenReturn(Optional.empty());

        Friend result = friendService.createFriend(friend);

        assertNotNull(result);
        assertEquals("friend-1", result.getId());
        assertEquals("John Doe", result.getDisplayName());
        verify(friendRepository, times(1)).save(friend);
    }

    @Test
    @DisplayName("Should throw exception when creating friend with null userId")
    void shouldThrowExceptionWhenCreatingFriendWithNullUserId() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId(null)
                .displayName("John Doe")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendService.createFriend(friend);
        });

        assertEquals("User ID is required", exception.getMessage());
        verify(friendRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when creating friend with empty displayName")
    void shouldThrowExceptionWhenCreatingFriendWithEmptyDisplayName() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendService.createFriend(friend);
        });

        assertEquals("Display name is required", exception.getMessage());
        verify(friendRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get friends by userId")
    void shouldGetFriendsByUserId() {
        String userId = "user-1";
        List<Friend> friends = Arrays.asList(
                Friend.builder().id("friend-1").userId(userId).displayName("John Doe").build(),
                Friend.builder().id("friend-2").userId(userId).displayName("Jane Smith").build()
        );

        when(friendRepository.findByUserId(userId)).thenReturn(friends);

        List<Friend> result = friendService.getFriendsByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getDisplayName());
        assertEquals("Jane Smith", result.get(1).getDisplayName());
        verify(friendRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should get friend by id")
    void shouldGetFriendById() {
        String friendId = "friend-1";
        Friend friend = Friend.builder()
                .id(friendId)
                .userId("user-1")
                .displayName("John Doe")
                .build();

        when(friendRepository.findById(friendId)).thenReturn(Optional.of(friend));

        Optional<Friend> result = friendService.getFriendById(friendId);

        assertTrue(result.isPresent());
        assertEquals(friendId, result.get().getId());
        assertEquals("John Doe", result.get().getDisplayName());
        verify(friendRepository, times(1)).findById(friendId);
    }

    @Test
    @DisplayName("Should return empty when friend not found")
    void shouldReturnEmptyWhenFriendNotFound() {
        String friendId = "nonexistent";
        when(friendRepository.findById(friendId)).thenReturn(Optional.empty());

        Optional<Friend> result = friendService.getFriendById(friendId);

        assertFalse(result.isPresent());
        verify(friendRepository, times(1)).findById(friendId);
    }

    @Test
    @DisplayName("Should update friend successfully")
    void shouldUpdateFriendSuccessfully() {
        String friendId = "friend-1";
        Friend existingFriend = Friend.builder()
                .id(friendId)
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend updatedFriend = Friend.builder()
                .id(friendId)
                .userId("user-1")
                .displayName("Johnny Doe")
                .venmoHandle("@johnny")
                .build();

        when(friendRepository.findById(friendId)).thenReturn(Optional.of(existingFriend));
        when(friendRepository.update(any(Friend.class))).thenReturn(updatedFriend);
        when(friendRepository.findByUserId("user-1")).thenReturn(Arrays.asList());
        when(userProfileService.getProfileByUserId("user-1")).thenReturn(Optional.empty());

        Friend result = friendService.updateFriend(friendId, updatedFriend);

        assertNotNull(result);
        assertEquals("Johnny Doe", result.getDisplayName());
        assertEquals("@johnny", result.getVenmoHandle());
        verify(friendRepository, times(1)).findById(friendId);
        verify(friendRepository, times(1)).update(any(Friend.class));
    }

    @Test
    @DisplayName("Should throw exception when updating nonexistent friend")
    void shouldThrowExceptionWhenUpdatingNonexistentFriend() {
        String friendId = "nonexistent";
        Friend updatedFriend = Friend.builder()
                .id(friendId)
                .userId("user-1")
                .displayName("John Doe")
                .build();

        when(friendRepository.findById(friendId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendService.updateFriend(friendId, updatedFriend);
        });

        assertTrue(exception.getMessage().contains("Friend not found"));
        verify(friendRepository, times(1)).findById(friendId);
        verify(friendRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should preserve userId when updating friend")
    void shouldPreserveUserIdWhenUpdatingFriend() {
        String friendId = "friend-1";
        Friend existingFriend = Friend.builder()
                .id(friendId)
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend updatedFriend = Friend.builder()
                .id(friendId)
                .userId("different-user")
                .displayName("Johnny Doe")
                .build();

        when(friendRepository.findById(friendId)).thenReturn(Optional.of(existingFriend));
        when(friendRepository.update(any(Friend.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRepository.findByUserId("user-1")).thenReturn(Arrays.asList());
        when(userProfileService.getProfileByUserId("user-1")).thenReturn(Optional.empty());

        Friend result = friendService.updateFriend(friendId, updatedFriend);

        assertEquals("user-1", result.getUserId());
        assertEquals("Johnny Doe", result.getDisplayName());
        verify(friendRepository, times(1)).update(any(Friend.class));
    }

    @Test
    @DisplayName("Should delete friend successfully")
    void shouldDeleteFriendSuccessfully() {
        String friendId = "friend-1";
        doNothing().when(friendRepository).deleteById(friendId);

        friendService.deleteFriend(friendId);

        verify(friendRepository, times(1)).deleteById(friendId);
    }

    @Test
    @DisplayName("Should validate friend with trimmed whitespace displayName")
    void shouldValidateFriendWithTrimmedWhitespaceDisplayName() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("   ")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendService.createFriend(friend);
        });

        assertEquals("Display name is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when friend displayName duplicates user's displayName")
    void shouldThrowExceptionWhenDisplayNameDuplicatesUserDisplayName() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("Test User")
                .build();

        UserProfile userProfile = UserProfile.builder()
                .userId("user-1")
                .displayName("Test User")
                .build();

        when(userProfileService.getProfileByUserId("user-1")).thenReturn(Optional.of(userProfile));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendService.createFriend(friend);
        });

        assertEquals("Friend display name cannot be the same as your own display name", exception.getMessage());
        verify(friendRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when friend displayName duplicates another friend")
    void shouldThrowExceptionWhenDisplayNameDuplicatesAnotherFriend() {
        Friend existingFriend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend newFriend = Friend.builder()
                .userId("user-1")
                .displayName("John Doe")
                .build();

        when(userProfileService.getProfileByUserId("user-1")).thenReturn(Optional.empty());
        when(friendRepository.findByUserId("user-1")).thenReturn(Arrays.asList(existingFriend));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendService.createFriend(newFriend);
        });

        assertEquals("A friend with this display name already exists", exception.getMessage());
        verify(friendRepository, never()).save(any());
    }
}

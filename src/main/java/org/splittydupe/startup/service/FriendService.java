package org.splittydupe.startup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.Friend;
import org.splittydupe.startup.model.UserProfile;
import org.splittydupe.startup.repository.FriendRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserProfileService userProfileService;

    public Friend createFriend(Friend friend) {
        log.info("Creating new friend for user: {}", friend.getUserId());
        validateFriend(friend);
        return friendRepository.save(friend);
    }

    public List<Friend> getFriendsByUserId(String userId) {
        log.info("Retrieving friends for user: {}", userId);
        return friendRepository.findByUserId(userId);
    }

    public Optional<Friend> getFriendById(String id) {
        log.info("Retrieving friend by ID: {}", id);
        return friendRepository.findById(id);
    }

    public Friend updateFriend(String id, Friend updatedFriend) {
        log.info("Updating friend with ID: {}", id);

        Optional<Friend> existingFriend = friendRepository.findById(id);
        if (existingFriend.isEmpty()) {
            throw new IllegalArgumentException("Friend not found with ID: " + id);
        }

        updatedFriend.setId(id);
        updatedFriend.setUserId(existingFriend.get().getUserId());

        validateFriend(updatedFriend);
        return friendRepository.update(updatedFriend);
    }

    public void deleteFriend(String id) {
        log.info("Deleting friend with ID: {}", id);
        friendRepository.deleteById(id);
    }

    private void validateFriend(Friend friend) {
        if (friend.getUserId() == null || friend.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (friend.getDisplayName() == null || friend.getDisplayName().trim().isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }

        Optional<UserProfile> userProfile = userProfileService.getProfileByUserId(friend.getUserId());
        if (userProfile.isPresent() && userProfile.get().getDisplayName() != null) {
            if (friend.getDisplayName().trim().equalsIgnoreCase(userProfile.get().getDisplayName().trim())) {
                throw new IllegalArgumentException("Friend display name cannot be the same as your own display name");
            }
        }

        List<Friend> existingFriends = friendRepository.findByUserId(friend.getUserId());
        for (Friend existingFriend : existingFriends) {
            if (friend.getId() != null && friend.getId().equals(existingFriend.getId())) {
                continue;
            }
            if (existingFriend.getDisplayName() != null &&
                existingFriend.getDisplayName().trim().equalsIgnoreCase(friend.getDisplayName().trim())) {
                throw new IllegalArgumentException("A friend with this display name already exists");
            }
        }

        if (friend.getContactEmail() != null && !friend.getContactEmail().trim().isEmpty()) {
            if (!isValidEmail(friend.getContactEmail())) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}

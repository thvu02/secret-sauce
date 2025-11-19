package org.splittydupe.startup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.Friend;
import org.splittydupe.startup.repository.FriendRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {
    private final FriendRepository friendRepository;

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
        if (friend.getFirstName() == null || friend.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (friend.getLastName() == null || friend.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }
}

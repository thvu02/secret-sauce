package org.splittydupe.startup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.model.Friend;
import org.splittydupe.startup.service.FriendService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FriendController {
    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<?> getAllFriends(Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<Friend> friends = friendService.getFriendsByUserId(userId);
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            log.error("Error retrieving friends", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve friends: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFriendById(@PathVariable String id, Authentication authentication) {
        try {
            return friendService.getFriendById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Friend not found")));
        } catch (Exception e) {
            log.error("Error retrieving friend by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve friend: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createFriend(@RequestBody Friend friend, Authentication authentication) {
        try {
            String userId = authentication.getName();
            friend.setUserId(userId);
            friend.setId(null); // Ensure new ID is generated

            Friend savedFriend = friendService.createFriend(friend);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFriend);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating friend", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating friend", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create friend: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFriend(@PathVariable String id,
                                         @RequestBody Friend friend,
                                         Authentication authentication) {
        try {
            Friend updatedFriend = friendService.updateFriend(id, friend);
            return ResponseEntity.ok(updatedFriend);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating friend", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating friend with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update friend: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFriend(@PathVariable String id, Authentication authentication) {
        try {
            friendService.deleteFriend(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting friend with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete friend: " + e.getMessage()));
        }
    }
}

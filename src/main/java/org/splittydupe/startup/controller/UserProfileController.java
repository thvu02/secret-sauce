package org.splittydupe.startup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.model.UserProfile;
import org.splittydupe.startup.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String userId = authentication.getName();
            return userProfileService.getProfileByUserId(userId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Profile not found")));
        } catch (Exception e) {
            log.error("Error retrieving user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve profile: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody UserProfile profile, Authentication authentication) {
        try {
            String userId = authentication.getName();
            profile.setUserId(userId);

            UserProfile savedProfile = userProfileService.saveProfile(profile);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProfile);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create profile: " + e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile profile, Authentication authentication) {
        try {
            String userId = authentication.getName();
            UserProfile updatedProfile = userProfileService.updateProfile(userId, profile);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update profile: " + e.getMessage()));
        }
    }
}

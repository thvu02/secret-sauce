package org.splittydupe.startup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.UserProfile;
import org.splittydupe.startup.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfile saveProfile(UserProfile profile) {
        log.info("Saving user profile for user: {}", profile.getUserId());
        validateProfile(profile);
        return userProfileRepository.save(profile);
    }

    public Optional<UserProfile> getProfileByUserId(String userId) {
        log.info("Retrieving user profile for user: {}", userId);
        return userProfileRepository.findByUserId(userId);
    }

    public UserProfile updateProfile(String userId, UserProfile profile) {
        log.info("Updating user profile for user: {}", userId);
        profile.setUserId(userId);
        validateProfile(profile);
        return userProfileRepository.save(profile);
    }

    public void deleteProfile(String userId) {
        log.info("Deleting user profile for user: {}", userId);
        userProfileRepository.delete(userId);
    }

    private void validateProfile(UserProfile profile) {
        if (profile.getUserId() == null || profile.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}

package org.splittydupe.startup.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.exception.FirestoreException;
import org.splittydupe.startup.model.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserProfileRepository {
    private final Firestore firestore;

    @Value("${firestore.collection.userProfiles:user-profiles}")
    private String collection;

    public UserProfile save(UserProfile profile) {
        try {
            DocumentReference docRef = firestore.collection(collection)
                    .document(profile.getUserId());

            ApiFuture<WriteResult> future = docRef.set(profile);
            WriteResult result = future.get();

            log.info("User profile saved successfully. User ID: {}, Update time: {}",
                    profile.getUserId(), result.getUpdateTime());
            return profile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while saving user profile", e);
            throw new FirestoreException("Failed to save user profile: Thread interrupted", e);
        } catch (ExecutionException e) {
            log.error("Failed to save user profile to Firestore", e);
            throw new FirestoreException("Failed to save user profile: " + e.getMessage(), e);
        }
    }

    public Optional<UserProfile> findByUserId(String userId) {
        try {
            DocumentReference docRef = firestore.collection(collection).document(userId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                UserProfile profile = document.toObject(UserProfile.class);
                log.info("User profile retrieved for user: {}", userId);
                return Optional.ofNullable(profile);
            } else {
                log.info("No profile found for user: {}", userId);
                return Optional.empty();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while retrieving user profile", e);
            throw new FirestoreException("Failed to retrieve user profile: Thread interrupted", e);
        } catch (ExecutionException e) {
            log.error("Failed to retrieve user profile from Firestore", e);
            throw new FirestoreException("Failed to retrieve user profile: " + e.getMessage(), e);
        }
    }

    public void delete(String userId) {
        try {
            DocumentReference docRef = firestore.collection(collection).document(userId);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get();

            log.info("User profile deleted for user: {}", userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while deleting user profile", e);
            throw new FirestoreException("Failed to delete user profile: Thread interrupted", e);
        } catch (ExecutionException e) {
            log.error("Failed to delete user profile from Firestore", e);
            throw new FirestoreException("Failed to delete user profile: " + e.getMessage(), e);
        }
    }
}

package org.splittydupe.startup.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.Friend;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FriendRepository {
    private static final String COLLECTION_NAME = "friends";
    private final Firestore firestore;

    public Friend save(Friend friend) {
        try {
            if (friend.getId() == null || friend.getId().isEmpty()) {
                friend.setId(UUID.randomUUID().toString());
            }

            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(friend.getId());
            docRef.set(friend).get();

            log.info("Successfully saved friend with ID: {}", friend.getId());
            return friend;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving friend to Firestore", e);
            throw new RuntimeException("Failed to save friend", e);
        }
    }

    public Optional<Friend> findById(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            var docSnapshot = docRef.get().get();

            if (docSnapshot.exists()) {
                Friend friend = docSnapshot.toObject(Friend.class);
                log.info("Found friend with ID: {}", id);
                return Optional.ofNullable(friend);
            }

            log.info("Friend not found with ID: {}", id);
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding friend by ID: {}", id, e);
            throw new RuntimeException("Failed to find friend", e);
        }
    }

    public List<Friend> findByUserId(String userId) {
        try {
            var query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            List<Friend> friends = new ArrayList<>();
            for (QueryDocumentSnapshot document : query.getDocuments()) {
                friends.add(document.toObject(Friend.class));
            }

            log.info("Found {} friends for user ID: {}", friends.size(), userId);
            return friends;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding friends for user ID: {}", userId, e);
            throw new RuntimeException("Failed to find friends for user", e);
        }
    }

    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION_NAME).document(id).delete().get();
            log.info("Successfully deleted friend with ID: {}", id);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting friend with ID: {}", id, e);
            throw new RuntimeException("Failed to delete friend", e);
        }
    }

    public Friend update(Friend friend) {
        if (friend.getId() == null || friend.getId().isEmpty()) {
            throw new IllegalArgumentException("Friend ID cannot be null or empty for update");
        }
        return save(friend);
    }
}

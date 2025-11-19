package org.splittydupe.startup.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private static final String USERS_COLLECTION = "users";

    private final Firestore firestore;

    public boolean save(User user) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(user.getUid());
            ApiFuture<WriteResult> future = docRef.set(user);
            WriteResult result = future.get();
            log.info("User saved successfully. UID: {}, Update time: {}", user.getUid(), result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to save user to database. UID: {}", user.getUid(), e);
            throw new RuntimeException("Failed to save user to database: " + e.getMessage(), e);
        }
    }

    public Optional<User> findById(String uid) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(uid);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                log.warn("User not found. UID: {}", uid);
                return Optional.empty();
            }

            log.info("User retrieved successfully. UID: {}", uid);
            return Optional.ofNullable(document.toObject(User.class));
        } catch (Exception e) {
            log.error("Failed to fetch user from database. UID: {}", uid, e);
            throw new RuntimeException("Failed to fetch user from database: " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", email)
                    .get();

            QuerySnapshot querySnapshot = future.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

            if (documents.isEmpty()) {
                log.info("No user found with email: {}", email);
                return Optional.empty();
            }

            User user = documents.get(0).toObject(User.class);
            log.info("User retrieved successfully by email. Email: {}", email);
            return Optional.of(user);
        } catch (Exception e) {
            log.error("Failed to fetch user by email. Email: {}", email, e);
            throw new RuntimeException("Failed to fetch user by email: " + e.getMessage(), e);
        }
    }

    public boolean update(User user) {
        return save(user);
    }

    public boolean delete(String uid) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(uid);
            ApiFuture<WriteResult> future = docRef.delete();
            WriteResult result = future.get();
            log.info("User deleted successfully. UID: {}, Delete time: {}", uid, result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to delete user from database. UID: {}", uid, e);
            throw new RuntimeException("Failed to delete user from database: " + e.getMessage(), e);
        }
    }
}

package org.splittydupe.startup.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.VerificationToken;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VerificationTokenRepository {

    private static final String TOKENS_COLLECTION = "verification_tokens";

    private final Firestore firestore;

    public boolean save(VerificationToken token) {
        try {
            DocumentReference docRef = firestore.collection(TOKENS_COLLECTION).document(token.getUid());
            ApiFuture<WriteResult> future = docRef.set(token);
            WriteResult result = future.get();
            log.info("Verification token saved successfully. UID: {}, Update time: {}", token.getUid(), result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to save verification token to database. UID: {}", token.getUid(), e);
            throw new RuntimeException("Failed to save verification token to database: " + e.getMessage(), e);
        }
    }

    public Optional<VerificationToken> findByToken(String token) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(TOKENS_COLLECTION)
                    .whereEqualTo("token", token)
                    .get();

            QuerySnapshot querySnapshot = future.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

            if (documents.isEmpty()) {
                log.info("No verification token found: {}", token);
                return Optional.empty();
            }

            VerificationToken verificationToken = documents.get(0).toObject(VerificationToken.class);
            log.info("Verification token retrieved successfully. Token: {}", token);
            return Optional.of(verificationToken);
        } catch (Exception e) {
            log.error("Failed to fetch verification token. Token: {}", token, e);
            throw new RuntimeException("Failed to fetch verification token: " + e.getMessage(), e);
        }
    }

    public Optional<VerificationToken> findById(String uid) {
        try {
            DocumentReference docRef = firestore.collection(TOKENS_COLLECTION).document(uid);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                log.warn("Verification token not found. UID: {}", uid);
                return Optional.empty();
            }

            log.info("Verification token retrieved successfully. UID: {}", uid);
            return Optional.ofNullable(document.toObject(VerificationToken.class));
        } catch (Exception e) {
            log.error("Failed to fetch verification token from database. UID: {}", uid, e);
            throw new RuntimeException("Failed to fetch verification token from database: " + e.getMessage(), e);
        }
    }

    public boolean update(VerificationToken token) {
        return save(token);
    }

    public boolean delete(String uid) {
        try {
            DocumentReference docRef = firestore.collection(TOKENS_COLLECTION).document(uid);
            ApiFuture<WriteResult> future = docRef.delete();
            WriteResult result = future.get();
            log.info("Verification token deleted successfully. UID: {}, Delete time: {}", uid, result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to delete verification token from database. UID: {}", uid, e);
            throw new RuntimeException("Failed to delete verification token from database: " + e.getMessage(), e);
        }
    }
}

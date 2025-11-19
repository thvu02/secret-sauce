package org.splittydupe.startup.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.Receipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReceiptRepository implements IReceiptRepository {

    private final Firestore firestore;

    @Value("${TRANSACTION_COLLECTION}")
    private String collection;

    public boolean save(Receipt receipt) {
        try {
            DocumentReference docRef = firestore.collection(collection).document(receipt.getUid());
            ApiFuture<WriteResult> future = docRef.set(receipt);
            WriteResult result = future.get();
            log.info("Receipt saved successfully. UID: {}, Update time: {}", receipt.getUid(), result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to save receipt to database. UID: {}", receipt.getUid(), e);
            throw new RuntimeException("Failed to save receipt to database: " + e.getMessage(), e);
        }
    }

    public Receipt findById(String uid) {
        try {
            DocumentReference docRef = firestore.collection(collection).document(uid);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                log.warn("Receipt not found. UID: {}", uid);
                return null;
            }

            log.info("Receipt retrieved successfully. UID: {}", uid);
            return document.toObject(Receipt.class);
        } catch (Exception e) {
            log.error("Failed to fetch receipt from database. UID: {}", uid, e);
            throw new RuntimeException("Failed to fetch receipt from database: " + e.getMessage(), e);
        }
    }

    public java.util.List<Receipt> findByUserId(String userId) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(collection)
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    .get();

            QuerySnapshot querySnapshot = future.get();
            java.util.List<Receipt> receipts = new java.util.ArrayList<>();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                receipts.add(document.toObject(Receipt.class));
            }

            log.info("Found {} receipts for user: {} (ordered by createdAt desc)", receipts.size(), userId);
            return receipts;
        } catch (Exception e) {
            log.error("Failed to fetch receipts for user: {}", userId, e);
            throw new RuntimeException("Failed to fetch receipts for user: " + e.getMessage(), e);
        }
    }

    public java.util.List<Receipt> findExpiredAnonymousReceipts() {
        try {
            com.google.cloud.Timestamp now = com.google.cloud.Timestamp.now();

            ApiFuture<QuerySnapshot> future = firestore.collection(collection)
                    .whereEqualTo("userId", "anonymous")
                    .whereLessThan("expiresAt", now)
                    .get();

            QuerySnapshot querySnapshot = future.get();
            java.util.List<Receipt> receipts = new java.util.ArrayList<>();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                receipts.add(document.toObject(Receipt.class));
            }

            log.info("Found {} expired anonymous receipts", receipts.size());
            return receipts;
        } catch (Exception e) {
            log.error("Failed to fetch expired anonymous receipts", e);
            throw new RuntimeException("Failed to fetch expired anonymous receipts: " + e.getMessage(), e);
        }
    }

    public boolean delete(String uid) {
        try {
            DocumentReference docRef = firestore.collection(collection).document(uid);
            ApiFuture<WriteResult> future = docRef.delete();
            WriteResult result = future.get();
            log.info("Receipt deleted successfully. UID: {}, Delete time: {}", uid, result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Failed to delete receipt. UID: {}", uid, e);
            throw new RuntimeException("Failed to delete receipt: " + e.getMessage(), e);
        }
    }
}

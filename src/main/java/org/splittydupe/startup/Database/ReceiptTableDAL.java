package org.splittydupe.startup.Database;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.DocumentReference;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;

import java.io.IOException;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReceiptTableDAL {

    @Value("${GCP_PROJECT_ID}")
    private String projectId;
    @Value("${TRANSACTION_DATABASE_ID}")
    private String databaseId;
    @Value("${TRANSACTION_COLLECTION}")
    private String collection;
    protected Firestore db;
    
    @PostConstruct
    public void initDB() { 
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                                                                .setProjectId(projectId)
                                                                .setCredentials(credentials)
                                                                .setDatabaseId(databaseId)
                                                                .build();
            db = firestoreOptions.getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google credentials: " + e.getMessage(), e);
        }
    }

    public boolean saveReceipt(Receipt receipt) {
        try {
            DocumentReference docRef = db.collection(collection).document(receipt.getUid());
            
            ApiFuture<WriteResult> future = docRef.set(receipt);
            
            WriteResult result = future.get();
            System.out.println("Receipt saved successfully. Update time: " + result.getUpdateTime());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save receipt to database: " + e.getMessage(), e);
        }
    }

    public Receipt getReceipt(String uid) {
        try {
            DocumentReference docRef = db.collection(collection).document(uid);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            return document.toObject(Receipt.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Receipt from database: ", e);
        }
    }
}

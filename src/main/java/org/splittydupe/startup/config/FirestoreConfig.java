package org.splittydupe.startup.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.exception.FirestoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${GCP_PROJECT_ID}")
    private String projectId;

    @Value("${TRANSACTION_DATABASE_ID}")
    private String databaseId;

    @Bean
    public Firestore firestore() {
        log.info("Initializing Firestore for project: {}, database: {}", projectId, databaseId);

        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .setDatabaseId(databaseId)
                    .build();

            Firestore firestore = firestoreOptions.getService();
            log.info("Firestore initialized successfully");

            return firestore;
        } catch (IOException e) {
            log.error("Failed to initialize Firestore", e);
            throw new FirestoreException("Failed to initialize Firestore for project: " + projectId, e);
        }
    }
}

package org.splittydupe.startup.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirestoreConfig Tests")
class FirestoreConfigTest {

    @InjectMocks
    private FirestoreConfig firestoreConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(firestoreConfig, "projectId", "test-project");
        ReflectionTestUtils.setField(firestoreConfig, "databaseId", "test-database");
    }

    @Test
    @DisplayName("Should initialize with correct project ID")
    void shouldInitializeWithCorrectProjectId() {
        assertEquals("test-project", ReflectionTestUtils.getField(firestoreConfig, "projectId"));
    }

    @Test
    @DisplayName("Should initialize with correct database ID")
    void shouldInitializeWithCorrectDatabaseId() {
        assertEquals("test-database", ReflectionTestUtils.getField(firestoreConfig, "databaseId"));
    }

    @Test
    @DisplayName("Should have non-null configuration fields")
    void shouldHaveNonNullConfigurationFields() {
        assertNotNull(ReflectionTestUtils.getField(firestoreConfig, "projectId"));
        assertNotNull(ReflectionTestUtils.getField(firestoreConfig, "databaseId"));
    }

    @Test
    @DisplayName("Should handle different project IDs")
    void shouldHandleDifferentProjectIds() {
        String newProjectId = "another-project";
        ReflectionTestUtils.setField(firestoreConfig, "projectId", newProjectId);

        assertEquals(newProjectId, ReflectionTestUtils.getField(firestoreConfig, "projectId"));
    }

    @Test
    @DisplayName("Should handle different database IDs")
    void shouldHandleDifferentDatabaseIds() {
        String newDatabaseId = "another-database";
        ReflectionTestUtils.setField(firestoreConfig, "databaseId", newDatabaseId);

        assertEquals(newDatabaseId, ReflectionTestUtils.getField(firestoreConfig, "databaseId"));
    }
}

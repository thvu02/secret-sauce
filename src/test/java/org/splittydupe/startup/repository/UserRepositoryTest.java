package org.splittydupe.startup.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference collectionReference;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private ApiFuture<WriteResult> writeResultFuture;

    @Mock
    private ApiFuture<DocumentSnapshot> documentSnapshotFuture;

    @Mock
    private ApiFuture<QuerySnapshot> querySnapshotFuture;

    @Mock
    private WriteResult writeResult;

    @Mock
    private DocumentSnapshot documentSnapshot;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private Query query;

    @InjectMocks
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .uid("user-123")
                .email("test@example.com")
                .passwordHash("hashed")
                .emailVerified(true)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should save user successfully")
    void shouldSaveUserSuccessfully() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(testUser.getUid())).thenReturn(documentReference);
        when(documentReference.set(testUser)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = userRepository.save(testUser);

        assertTrue(result);
        verify(documentReference, times(1)).set(testUser);
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(testUser.getUid())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(User.class)).thenReturn(testUser);

        Optional<User> result = userRepository.findById(testUser.getUid());

        assertTrue(result.isPresent());
        assertEquals(testUser.getUid(), result.get().getUid());
    }

    @Test
    @DisplayName("Should return empty when user not found by id")
    void shouldReturnEmptyWhenUserNotFoundById() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document("nonexistent")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        Optional<User> result = userRepository.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() throws Exception {
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.toObject(User.class)).thenReturn(testUser);

        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("email", testUser.getEmail())).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(doc));

        Optional<User> result = userRepository.findByEmail(testUser.getEmail());

        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("email", "notfound@example.com")).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(new ArrayList<>());

        Optional<User> result = userRepository.findByEmail("notfound@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should update user")
    void shouldUpdateUser() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(testUser.getUid())).thenReturn(documentReference);
        when(documentReference.set(testUser)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = userRepository.update(testUser);

        assertTrue(result);
        verify(documentReference, times(1)).set(testUser);
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(testUser.getUid())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = userRepository.delete(testUser.getUid());

        assertTrue(result);
        verify(documentReference, times(1)).delete();
    }

    @Test
    @DisplayName("Should throw exception when save fails")
    void shouldThrowExceptionWhenSaveFails() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(testUser.getUid())).thenReturn(documentReference);
        when(documentReference.set(testUser)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        assertThrows(RuntimeException.class, () -> userRepository.save(testUser));
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() throws Exception {
        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.document(testUser.getUid())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        assertThrows(RuntimeException.class, () -> userRepository.delete(testUser.getUid()));
    }
}

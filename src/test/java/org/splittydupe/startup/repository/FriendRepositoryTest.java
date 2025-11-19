package org.splittydupe.startup.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.model.Friend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendRepository Tests")
class FriendRepositoryTest {

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
    private DocumentSnapshot documentSnapshot;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private Query query;

    @InjectMocks
    private FriendRepository friendRepository;

    private Friend testFriend;

    @BeforeEach
    void setUp() {
        testFriend = Friend.builder()
                .id("friend-123")
                .userId("user-456")
                .firstName("John")
                .lastName("Doe")
                .venmoHandle("@johndoe")
                .build();
    }

    @Test
    @DisplayName("Should save friend with existing ID")
    void shouldSaveFriendWithExistingId() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(testFriend.getId())).thenReturn(documentReference);
        when(documentReference.set(testFriend)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        Friend result = friendRepository.save(testFriend);

        assertNotNull(result);
        assertEquals(testFriend.getId(), result.getId());
        verify(documentReference, times(1)).set(testFriend);
    }

    @Test
    @DisplayName("Should generate ID when saving friend without ID")
    void shouldGenerateIdWhenSavingFriendWithoutId() throws Exception {
        Friend friendWithoutId = Friend.builder()
                .userId("user-456")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Friend.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        Friend result = friendRepository.save(friendWithoutId);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertFalse(result.getId().isEmpty());
    }

    @Test
    @DisplayName("Should find friend by ID")
    void shouldFindFriendById() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(testFriend.getId())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Friend.class)).thenReturn(testFriend);

        Optional<Friend> result = friendRepository.findById(testFriend.getId());

        assertTrue(result.isPresent());
        assertEquals(testFriend.getId(), result.get().getId());
        assertEquals("John", result.get().getFirstName());
    }

    @Test
    @DisplayName("Should return empty when friend not found")
    void shouldReturnEmptyWhenFriendNotFound() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document("nonexistent")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        Optional<Friend> result = friendRepository.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find friends by userId")
    void shouldFindFriendsByUserId() throws Exception {
        String userId = "user-456";
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.toObject(Friend.class)).thenReturn(testFriend);

        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("userId", userId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(doc));

        List<Friend> results = friendRepository.findByUserId(userId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("John", results.get(0).getFirstName());
    }

    @Test
    @DisplayName("Should return empty list when no friends found for user")
    void shouldReturnEmptyListWhenNoFriendsFoundForUser() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("userId", "user-no-friends")).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(new ArrayList<>());

        List<Friend> results = friendRepository.findByUserId("user-no-friends");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should delete friend by ID")
    void shouldDeleteFriendById() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(testFriend.getId())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        friendRepository.deleteById(testFriend.getId());

        verify(documentReference, times(1)).delete();
    }

    @Test
    @DisplayName("Should update friend")
    void shouldUpdateFriend() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(testFriend.getId())).thenReturn(documentReference);
        when(documentReference.set(testFriend)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        Friend result = friendRepository.update(testFriend);

        assertNotNull(result);
        assertEquals(testFriend.getId(), result.getId());
        verify(documentReference, times(1)).set(testFriend);
    }

    @Test
    @DisplayName("Should throw exception when updating friend without ID")
    void shouldThrowExceptionWhenUpdatingFriendWithoutId() {
        Friend friendWithoutId = Friend.builder()
                .userId("user-456")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            friendRepository.update(friendWithoutId);
        });

        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    @DisplayName("Should throw exception when save fails")
    void shouldThrowExceptionWhenSaveFails() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(testFriend.getId())).thenReturn(documentReference);
        when(documentReference.set(testFriend)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new ExecutionException(new RuntimeException("Firestore error")));

        assertThrows(RuntimeException.class, () -> friendRepository.save(testFriend));
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() throws Exception {
        when(firestore.collection("friends")).thenReturn(collectionReference);
        when(collectionReference.document(testFriend.getId())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new ExecutionException(new RuntimeException("Firestore error")));

        assertThrows(RuntimeException.class, () -> friendRepository.deleteById(testFriend.getId()));
    }
}

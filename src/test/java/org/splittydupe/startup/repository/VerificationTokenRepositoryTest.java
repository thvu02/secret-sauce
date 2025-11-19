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
import org.splittydupe.startup.model.VerificationToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationTokenRepository Tests")
class VerificationTokenRepositoryTest {

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
    private VerificationTokenRepository tokenRepository;

    private VerificationToken testToken;

    @BeforeEach
    void setUp() {
        testToken = VerificationToken.builder()
                .uid("token-123")
                .token("verification-token-abc")
                .userId("user-456")
                .userEmail("test@example.com")
                .tokenType("email_verification")
                .expiryDate(Timestamp.now())
                .used(false)
                .build();
    }

    @Test
    @DisplayName("Should save token successfully")
    void shouldSaveTokenSuccessfully() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(testToken.getUid())).thenReturn(documentReference);
        when(documentReference.set(testToken)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = tokenRepository.save(testToken);

        assertTrue(result);
        verify(documentReference, times(1)).set(testToken);
    }

    @Test
    @DisplayName("Should find token by token value")
    void shouldFindTokenByTokenValue() throws Exception {
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.toObject(VerificationToken.class)).thenReturn(testToken);

        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("token", testToken.getToken())).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(doc));

        Optional<VerificationToken> result = tokenRepository.findByToken(testToken.getToken());

        assertTrue(result.isPresent());
        assertEquals(testToken.getToken(), result.get().getToken());
    }

    @Test
    @DisplayName("Should return empty when token not found by value")
    void shouldReturnEmptyWhenTokenNotFoundByValue() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("token", "nonexistent")).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(new ArrayList<>());

        Optional<VerificationToken> result = tokenRepository.findByToken("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find token by ID")
    void shouldFindTokenById() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(testToken.getUid())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(VerificationToken.class)).thenReturn(testToken);

        Optional<VerificationToken> result = tokenRepository.findById(testToken.getUid());

        assertTrue(result.isPresent());
        assertEquals(testToken.getUid(), result.get().getUid());
    }

    @Test
    @DisplayName("Should return empty when token not found by ID")
    void shouldReturnEmptyWhenTokenNotFoundById() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document("nonexistent")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        Optional<VerificationToken> result = tokenRepository.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should update token")
    void shouldUpdateToken() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(testToken.getUid())).thenReturn(documentReference);
        when(documentReference.set(testToken)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = tokenRepository.update(testToken);

        assertTrue(result);
        verify(documentReference, times(1)).set(testToken);
    }

    @Test
    @DisplayName("Should delete token successfully")
    void shouldDeleteTokenSuccessfully() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(testToken.getUid())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = tokenRepository.delete(testToken.getUid());

        assertTrue(result);
        verify(documentReference, times(1)).delete();
    }

    @Test
    @DisplayName("Should throw exception when save fails")
    void shouldThrowExceptionWhenSaveFails() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(testToken.getUid())).thenReturn(documentReference);
        when(documentReference.set(testToken)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        assertThrows(RuntimeException.class, () -> tokenRepository.save(testToken));
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() throws Exception {
        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(testToken.getUid())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        assertThrows(RuntimeException.class, () -> tokenRepository.delete(testToken.getUid()));
    }

    @Test
    @DisplayName("Should handle different token types")
    void shouldHandleDifferentTokenTypes() throws Exception {
        VerificationToken passwordResetToken = VerificationToken.builder()
                .uid("token-password-reset")
                .token("reset-token-xyz")
                .userId("user-789")
                .userEmail("reset@example.com")
                .tokenType("password_reset")
                .expiryDate(Timestamp.now())
                .used(false)
                .build();

        when(firestore.collection("verification_tokens")).thenReturn(collectionReference);
        when(collectionReference.document(passwordResetToken.getUid())).thenReturn(documentReference);
        when(documentReference.set(passwordResetToken)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = tokenRepository.save(passwordResetToken);

        assertTrue(result);
        verify(documentReference, times(1)).set(passwordResetToken);
    }
}

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
import org.splittydupe.startup.model.Receipt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptRepository Tests")
class ReceiptRepositoryTest {

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
    private ReceiptRepository receiptRepository;

    private Receipt testReceipt;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(receiptRepository, "collection", "receipts");

        testReceipt = Receipt.builder()
                .uid("receipt-123")
                .vendor("Test Restaurant")
                .userId("user-456")
                .total(100.00)
                .build();
    }

    @Test
    @DisplayName("Should save receipt successfully")
    void shouldSaveReceiptSuccessfully() throws Exception {
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.document(testReceipt.getUid())).thenReturn(documentReference);
        when(documentReference.set(testReceipt)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = receiptRepository.save(testReceipt);

        assertTrue(result);
        verify(firestore, times(1)).collection("receipts");
        verify(documentReference, times(1)).set(testReceipt);
    }

    @Test
    @DisplayName("Should throw exception when save fails")
    void shouldThrowExceptionWhenSaveFails() throws Exception {
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.document(testReceipt.getUid())).thenReturn(documentReference);
        when(documentReference.set(testReceipt)).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            receiptRepository.save(testReceipt);
        });

        assertTrue(exception.getMessage().contains("Failed to save receipt"));
    }

    @Test
    @DisplayName("Should find receipt by id")
    void shouldFindReceiptById() throws Exception {
        String receiptId = "receipt-123";
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.document(receiptId)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Receipt.class)).thenReturn(testReceipt);

        Receipt result = receiptRepository.findById(receiptId);

        assertNotNull(result);
        assertEquals(receiptId, result.getUid());
        verify(documentSnapshot, times(1)).toObject(Receipt.class);
    }

    @Test
    @DisplayName("Should return null when receipt not found")
    void shouldReturnNullWhenReceiptNotFound() throws Exception {
        String receiptId = "nonexistent";
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.document(receiptId)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        Receipt result = receiptRepository.findById(receiptId);

        assertNull(result);
        verify(documentSnapshot, never()).toObject(any());
    }

    @Test
    @DisplayName("Should find receipts by userId")
    void shouldFindReceiptsByUserId() throws Exception {
        String userId = "user-123";
        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.toObject(Receipt.class)).thenReturn(testReceipt);
        documents.add(doc);

        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("userId", userId)).thenReturn(query);
        when(query.orderBy("createdAt", Query.Direction.DESCENDING)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(documents);

        List<Receipt> results = receiptRepository.findByUserId(userId);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(collectionReference, times(1)).whereEqualTo("userId", userId);
    }

    @Test
    @DisplayName("Should find expired anonymous receipts")
    void shouldFindExpiredAnonymousReceipts() throws Exception {
        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.toObject(Receipt.class)).thenReturn(testReceipt);
        documents.add(doc);

        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo(eq("userId"), eq("anonymous"))).thenReturn(query);
        when(query.whereLessThan(eq("expiresAt"), any(Timestamp.class))).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(documents);

        List<Receipt> results = receiptRepository.findExpiredAnonymousReceipts();

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(collectionReference, times(1)).whereEqualTo("userId", "anonymous");
    }

    @Test
    @DisplayName("Should delete receipt successfully")
    void shouldDeleteReceiptSuccessfully() throws Exception {
        String receiptId = "receipt-123";
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.document(receiptId)).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.now());

        boolean result = receiptRepository.delete(receiptId);

        assertTrue(result);
        verify(documentReference, times(1)).delete();
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() throws Exception {
        String receiptId = "receipt-123";
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.document(receiptId)).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            receiptRepository.delete(receiptId);
        });

        assertTrue(exception.getMessage().contains("Failed to delete receipt"));
    }

    @Test
    @DisplayName("Should handle empty result set for findByUserId")
    void shouldHandleEmptyResultSetForFindByUserId() throws Exception {
        String userId = "user-no-receipts";
        when(firestore.collection("receipts")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("userId", userId)).thenReturn(query);
        when(query.orderBy("createdAt", Query.Direction.DESCENDING)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(new ArrayList<>());

        List<Receipt> results = receiptRepository.findByUserId(userId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}

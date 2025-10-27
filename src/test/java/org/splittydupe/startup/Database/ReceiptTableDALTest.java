package org.splittydupe.startup.Database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.CollectionReference;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
public class ReceiptTableDALTest {

    @InjectMocks
    private ReceiptTableDAL dal;
    
    @Mock
    private Firestore mockDb;
    
    @Mock
    private CollectionReference collectionReference;
    
    @Mock
    private DocumentReference documentReference;

    @BeforeEach
    public void setup() throws Exception {
        Field collectionField = ReceiptTableDAL.class.getDeclaredField("collection");
        collectionField.setAccessible(true);
        collectionField.set(dal, "receipts");
        
        when(mockDb.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
    }

    @Test
    public void testSaveReceiptSuccess() throws Exception {
        Receipt receipt = new Receipt();
        receipt.setUid("a12");
        
        @SuppressWarnings("unchecked")
        ApiFuture<WriteResult> mockFuture = mock(ApiFuture.class);
        WriteResult writeResult = mock(WriteResult.class);
        
        when(documentReference.set(receipt)).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(writeResult);
        when(collectionReference.document("a12")).thenReturn(documentReference);

        boolean result = dal.saveReceipt(receipt);
        
        assertTrue(result);
        verify(collectionReference).document("a12");
        verify(documentReference).set(receipt);
    }

    @Test
    public void testGetReceiptSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        ApiFuture<DocumentSnapshot> mockFuture = mock(ApiFuture.class);
        DocumentSnapshot document = mock(DocumentSnapshot.class);
        Receipt expectedReceipt = new Receipt();
        
        when(collectionReference.document("uid567")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(document);
        when(document.toObject(Receipt.class)).thenReturn(expectedReceipt);

        Receipt fetched = dal.getReceipt("uid567");
        
        assertSame(expectedReceipt, fetched);
        verify(collectionReference).document("uid567");
        verify(documentReference).get();
        verify(document).toObject(Receipt.class);
    }
}


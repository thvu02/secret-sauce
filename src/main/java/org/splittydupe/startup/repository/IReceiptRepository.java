package org.splittydupe.startup.repository;

import org.splittydupe.startup.model.Receipt;
import java.util.List;

public interface IReceiptRepository {

    /**
     * Save a receipt to the database
     * @param receipt the receipt to save
     * @return true if saved successfully
     */
    boolean save(Receipt receipt);

    /**
     * Find a receipt by its unique identifier
     * @param uid the receipt UID
     * @return the receipt, or null if not found
     */
    Receipt findById(String uid);

    /**
     * Find all receipts for a specific user
     * @param userId the user's unique identifier
     * @return list of receipts belonging to the user
     */
    List<Receipt> findByUserId(String userId);

    /**
     * Find all expired anonymous receipts
     * @return list of expired anonymous receipts
     */
    List<Receipt> findExpiredAnonymousReceipts();

    /**
     * Delete a receipt by its unique identifier
     * @param uid the receipt UID
     * @return true if deleted successfully
     */
    boolean delete(String uid);
}

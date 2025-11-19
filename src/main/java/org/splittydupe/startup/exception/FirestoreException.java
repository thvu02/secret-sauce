package org.splittydupe.startup.exception;

public class FirestoreException extends SplittyDupeException {

    public FirestoreException(String message) {
        super(message);
    }

    public FirestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirestoreException(Throwable cause) {
        super(cause);
    }
}

package org.splittydupe.startup.exception;

public class SplittyDupeException extends RuntimeException {

    public SplittyDupeException(String message) {
        super(message);
    }

    public SplittyDupeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SplittyDupeException(Throwable cause) {
        super(cause);
    }
}

package org.splittydupe.startup.exception;

public class OcrProcessingException extends SplittyDupeException {

    public OcrProcessingException(String message) {
        super(message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OcrProcessingException(Throwable cause) {
        super(cause);
    }
}

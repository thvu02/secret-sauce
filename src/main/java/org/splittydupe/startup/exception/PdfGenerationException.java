package org.splittydupe.startup.exception;

public class PdfGenerationException extends SplittyDupeException {

    public PdfGenerationException(String message) {
        super(message);
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PdfGenerationException(Throwable cause) {
        super(cause);
    }
}

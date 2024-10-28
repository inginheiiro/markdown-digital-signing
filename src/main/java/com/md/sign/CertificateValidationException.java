package com.md.sign;

/**
 * Exception thrown when certificate validation fails.
 */
public class CertificateValidationException extends Exception {

    public CertificateValidationException(String message) {
        super(message);
    }

    public CertificateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
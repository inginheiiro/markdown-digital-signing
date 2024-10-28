package com.md.sign;

/**
 * Represents the result of a signature verification operation.
 */
public record SignatureVerificationResult(
        boolean valid,
        String signerDN,
        String message
) {
    public SignatureVerificationResult {
        if (message == null) {
            message = valid ? "Signature is valid" : "Signature verification failed";
        }
    }
}
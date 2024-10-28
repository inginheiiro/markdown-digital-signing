package com.md.sign;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Holds the cryptographic materials needed for digital signatures.
 * This includes the private key for signing, the signer's certificate,
 * and the complete certificate chain.
 */
public record SigningMaterials(
        PrivateKey privateKey,
        X509Certificate certificate,
        List<X509Certificate> certificateChain
) {
    /**
     * Creates a new SigningMaterials instance with validation.
     */
    public SigningMaterials {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        if (certificateChain == null || certificateChain.isEmpty()) {
            throw new IllegalArgumentException("Certificate chain cannot be null or empty");
        }
        if (!certificateChain.get(0).equals(certificate)) {
            throw new IllegalArgumentException("First certificate in chain must be the signer's certificate");
        }

        // Make the certificateChain unmodifiable
        certificateChain = List.copyOf(certificateChain);
    }

    /**
     * Returns a string representation of the signing materials.
     * Note: Does not include sensitive information like the private key.
     */
    @Override
    public String toString() {
        return String.format("SigningMaterials{certificate=%s, chainLength=%d}",
                certificate.getSubjectX500Principal().getName(),
                certificateChain.size());
    }
}
package com.md.sign;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

public record DocumentSignature(String signature, String signerDN, Instant expirationDate, Map<String, String> metadata,
                                Instant signedAt) {
    public DocumentSignature(String signature, String signerDN,
                             Instant expirationDate, Map<String, String> metadata,
                             Instant signedAt) {
        this.signature = signature;
        this.signerDN = signerDN;
        this.expirationDate = expirationDate;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.signedAt = signedAt;
    }

    @Override
    public Map<String, String> metadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public String toString() {
        return String.format("DocumentSignature{signer='%s', signed=%s, expires=%s}",
                signerDN, signedAt, expirationDate);
    }
}
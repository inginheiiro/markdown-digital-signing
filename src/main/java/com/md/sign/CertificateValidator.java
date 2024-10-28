package com.md.sign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;

@Service
public class CertificateValidator {

    private static final Logger logger = LoggerFactory.getLogger(CertificateValidator.class);

    private final Set<TrustAnchor> trustAnchors;
    private final CertPathValidator validator;
    private final CertificateFactory certFactory;

    @Value("${signature.cert.validity.days:30}")
    private int certValidityDays;

    public CertificateValidator(ResourceLoader resourceLoader,
                                @Value("${signature.truststore.path:classpath:truststore.jks}") String truststorePath,
                                @Value("${signature.truststore.password:changeit}") String truststorePassword) {
        try {
            this.validator = CertPathValidator.getInstance("PKIX");
            this.certFactory = CertificateFactory.getInstance("X.509");

            // Try to load trust anchors, use empty set if truststore is not available
            Set<TrustAnchor> loadedAnchors;
            try {
                loadedAnchors = loadTrustAnchors(resourceLoader, truststorePath, truststorePassword);
            } catch (Exception e) {
                logger.warn("Could not load truststore, proceeding with empty trust anchors: {}", e.getMessage());
                loadedAnchors = new HashSet<>();
            }
            this.trustAnchors = Collections.unmodifiableSet(loadedAnchors);

        } catch (Exception e) {
            logger.error("Failed to initialize CertificateValidator", e);
            throw new RuntimeException("Failed to initialize certificate validation", e);
        }
    }

    /**
     * Validates a certificate chain.
     *
     * @param leafCert The end-entity certificate to validate
     * @throws CertificateValidationException if validation fails
     */
    public void validateCertificateChain(X509Certificate leafCert) throws CertificateValidationException {
        try {
            // If no trust anchors are configured, only validate the certificate itself
            if (trustAnchors.isEmpty()) {
                logger.warn("No trust anchors configured, performing basic certificate validation only");
                validateCertificateExpiry(leafCert);
                validateKeyUsage(leafCert);
                return;
            }

            // Create certification path
            List<X509Certificate> certList = Collections.singletonList(leafCert);
            CertPath certPath = certFactory.generateCertPath(certList);

            // Set up validation parameters
            PKIXParameters params = new PKIXParameters(trustAnchors);
            params.setRevocationEnabled(false); // Disable CRL checking for simplicity

            // Validate the certification path
            PKIXCertPathValidatorResult result =
                    (PKIXCertPathValidatorResult) validator.validate(certPath, params);

            // Additional validations
            validateCertificateExpiry(leafCert);
            validateKeyUsage(leafCert);

            logger.debug("Certificate chain validation successful for subject: {}",
                    leafCert.getSubjectX500Principal().getName());

        } catch (CertificateValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateValidationException("Certificate validation failed", e);
        }
    }

    /**
     * Validates if a certificate has expired or is not yet valid.
     */
    private void validateCertificateExpiry(X509Certificate cert) throws CertificateValidationException {
        try {
            cert.checkValidity();

            // Check if certificate will expire soon
            Date notAfter = cert.getNotAfter();
            if (notAfter.before(new Date(System.currentTimeMillis() +
                    (long) certValidityDays * 24 * 60 * 60 * 1000))) {
                logger.warn("Certificate will expire soon: {}",
                        cert.getSubjectX500Principal().getName());
            }

            // Log certificate validity period
            logger.debug("Certificate valid from {} to {} for subject: {}",
                    cert.getNotBefore(),
                    cert.getNotAfter(),
                    cert.getSubjectX500Principal().getName());

        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw new CertificateValidationException(
                    "Certificate is not valid at current time: " +
                            cert.getSubjectX500Principal().getName(), e);
        }
    }

    /**
     * Validates the key usage extension of a certificate.
     */
    private void validateKeyUsage(X509Certificate cert) throws CertificateValidationException {
        boolean[] keyUsage = cert.getKeyUsage();
        if (keyUsage == null) {
            throw new CertificateValidationException(
                    "No key usage extension present in certificate: " +
                            cert.getSubjectX500Principal().getName());
        }

        // Check for digital signature usage (position 0 in the key usage array)
        if (!keyUsage[0]) { // digitalSignature
            throw new CertificateValidationException(
                    "Certificate is not authorized for digital signatures: " +
                            cert.getSubjectX500Principal().getName());
        }

        // Log key usage for debugging
        logger.debug("Certificate key usage validated for subject: {}",
                cert.getSubjectX500Principal().getName());
    }

    /**
     * Extracts the certificate chain from a certificate.
     */
    private List<X509Certificate> buildCertificateChain(X509Certificate leafCert)
            throws CertificateValidationException {
        List<X509Certificate> chain = new ArrayList<>();
        try {
            chain.add(leafCert);

            // For self-signed certificates, the chain contains only the certificate itself
            if (isSelfSigned(leafCert)) {
                logger.debug("Certificate is self-signed: {}",
                        leafCert.getSubjectX500Principal().getName());
                return chain;
            }

            // For non-self-signed certificates, try to build the chain from trust anchors
            for (TrustAnchor anchor : trustAnchors) {
                if (isIssuer(anchor.getTrustedCert(), leafCert)) {
                    chain.add(anchor.getTrustedCert());
                    logger.debug("Added issuer to chain: {}",
                            anchor.getTrustedCert().getSubjectX500Principal().getName());
                    break;
                }
            }

            return chain;
        } catch (Exception e) {
            throw new CertificateValidationException(
                    "Failed to build certificate chain", e);
        }
    }

    /**
     * Checks if a certificate is self-signed.
     */
    private boolean isSelfSigned(X509Certificate cert) {
        try {
            return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
        } catch (Exception e) {
            logger.warn("Error checking if certificate is self-signed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if one certificate is the issuer of another.
     */
    private boolean isIssuer(X509Certificate issuer, X509Certificate cert) {
        return cert.getIssuerX500Principal().equals(issuer.getSubjectX500Principal());
    }

    /**
     * Loads trust anchors from a truststore file.
     */
    private Set<TrustAnchor> loadTrustAnchors(ResourceLoader resourceLoader,
                                              String truststorePath, String password) throws Exception {

        Set<TrustAnchor> anchors = new HashSet<>();

        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            Resource resource = resourceLoader.getResource(truststorePath);

            if (!resource.exists()) {
                logger.warn("Truststore file not found at: {}", truststorePath);
                return anchors;
            }

            try (InputStream is = resource.getInputStream()) {
                trustStore.load(is, password.toCharArray());

                Enumeration<String> aliases = trustStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (trustStore.isCertificateEntry(alias)) {
                        Certificate cert = trustStore.getCertificate(alias);
                        if (cert instanceof X509Certificate) {
                            anchors.add(new TrustAnchor((X509Certificate) cert, null));
                            logger.debug("Added trust anchor: {}",
                                    ((X509Certificate) cert).getSubjectX500Principal().getName());
                        }
                    }
                }
            }

            logger.info("Loaded {} trust anchors from truststore", anchors.size());

        } catch (Exception e) {
            logger.error("Error loading truststore: {}", e.getMessage());
            throw e;
        }

        return anchors;
    }
}
package com.md.sign;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DigitalSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(DigitalSignatureService.class);

    private final KeyStore keyStore;
    private final CertificateValidator certificateValidator;

    @Value("${signature.keystore.password}")
    private String keystorePassword;

    @Value("${signature.keystore.alias}")
    private String keystoreAlias;

    @Value("${signature.validity.days:365}")
    private int validityDays;

    @Autowired
    public DigitalSignatureService(KeyStore keyStore, CertificateValidator certificateValidator) {
        this.keyStore = keyStore;
        this.certificateValidator = certificateValidator;
    }

    public String signMarkdown(String markdownContent, Map<String, String> metadata) throws Exception {
        logger.debug("Starting markdown signing process");
        MarkdownDocument doc = MarkdownParser.parse(markdownContent);
        String contentToSign = doc.getContent().trim();

        SigningMaterials materials = getSigningMaterials();
        CMSSignedData signedData = createSignature(contentToSign, materials);

        DocumentSignature docSignature = new DocumentSignature(
                Base64.getEncoder().encodeToString(signedData.getEncoded()),
                materials.certificate().getSubjectX500Principal().getName(),
                Instant.now().plus(validityDays, ChronoUnit.DAYS),
                metadata,
                Instant.now()
        );

        doc.addSignature(docSignature);
        logger.debug("Document signed successfully");
        return MarkdownParser.serialize(doc);
    }

    public List<SignatureVerificationResult> verifySignatures(String markdownContent) {
        try {
            logger.debug("Starting signature verification process");
            MarkdownDocument doc = MarkdownParser.parse(markdownContent);
            List<SignatureVerificationResult> results = new ArrayList<>();

            if (doc.getSignatures().isEmpty()) {
                logger.warn("No signatures found in document");
                return Collections.singletonList(new SignatureVerificationResult(
                        false,
                        null,
                        "No signatures found in document"
                ));
            }

            String contentToVerify = doc.getContent().trim();
            logger.debug("Found {} signatures to verify", doc.getSignatures().size());

            for (DocumentSignature signature : doc.getSignatures()) {
                try {
                    results.add(verifySignature(contentToVerify, signature));
                } catch (Exception e) {
                    logger.error("Error verifying signature: {}", signature.signerDN(), e);
                    results.add(new SignatureVerificationResult(
                            false,
                            signature.signerDN(),
                            "Signature verification failed: " + e.getMessage()
                    ));
                }
            }

            return results;

        } catch (Exception e) {
            logger.error("Error during signature verification process", e);
            return Collections.singletonList(new SignatureVerificationResult(
                    false,
                    null,
                    "Failed to verify signatures: " + e.getMessage()
            ));
        }
    }

    private SignatureVerificationResult verifySignature(String content, DocumentSignature signature) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signature.signature());
            logger.debug("Verifying signature from: {}", signature.signerDN());

            CMSSignedData signedData = new CMSSignedData(
                    new CMSProcessableByteArray(content.getBytes(StandardCharsets.UTF_8)),
                    signatureBytes
            );

            SignerInformation signer = signedData.getSignerInfos().getSigners().iterator().next();
            X509Certificate signerCert = extractSignerCertificate(signedData, signer);

            certificateValidator.validateCertificateChain(signerCert);

            boolean isValid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider("BC")
                    .build(signerCert));

            logger.debug("Cryptographic verification result: {}", isValid);

            if (!isValid) {
                return new SignatureVerificationResult(
                        false,
                        signature.signerDN(),
                        "Invalid signature"
                );
            }

            if (signature.expirationDate() != null &&
                    Instant.now().isAfter(signature.expirationDate())) {
                logger.warn("Signature from {} has expired", signature.signerDN());
                return new SignatureVerificationResult(
                        false,
                        signature.signerDN(),
                        "Signature has expired"
                );
            }

            return new SignatureVerificationResult(
                    true,
                    signature.signerDN(),
                    "Signature is valid"
            );

        } catch (CMSException e) {
            logger.error("CMS error while verifying signature", e);
            return new SignatureVerificationResult(
                    false,
                    signature.signerDN(),
                    "Invalid signature format: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error verifying signature", e);
            return new SignatureVerificationResult(
                    false,
                    signature.signerDN(),
                    "Verification failed: " + e.getMessage()
            );
        }
    }

    private SigningMaterials getSigningMaterials() throws Exception {
        try {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                    keystoreAlias,
                    keystorePassword.toCharArray()
            );

            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keystoreAlias);

            if (privateKey == null || certificate == null) {
                throw new KeyStoreException(
                        "Required key or certificate not found for alias: " + keystoreAlias);
            }

            Certificate[] certChain = keyStore.getCertificateChain(keystoreAlias);
            if (certChain == null || certChain.length == 0) {
                certChain = new Certificate[]{certificate};
            }

            List<X509Certificate> certList = new ArrayList<>();
            for (Certificate cert : certChain) {
                certList.add((X509Certificate) cert);
            }

            return new SigningMaterials(privateKey, certificate, certList);

        } catch (Exception e) {
            logger.error("Error loading signing materials", e);
            throw new KeyStoreException("Failed to load signing materials: " + e.getMessage(), e);
        }
    }

    private CMSSignedData createSignature(String content, SigningMaterials materials) throws Exception {
        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            CMSTypedData cmsData = new CMSProcessableByteArray(contentBytes);

            Store certs = new JcaCertStore(materials.certificateChain());

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(materials.privateKey());

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                    )
                            .build(contentSigner, materials.certificate())
            );
            generator.addCertificates(certs);

            return generator.generate(cmsData, true);

        } catch (Exception e) {
            logger.error("Error creating signature", e);
            throw new CMSException("Failed to create signature: " + e.getMessage(), e);
        }
    }

    private X509Certificate extractSignerCertificate(CMSSignedData signedData,
                                                     SignerInformation signer) throws Exception {
        Collection<X509CertificateHolder> certCollection = signedData.getCertificates().getMatches(signer.getSID());

        if (certCollection.isEmpty()) {
            throw new CertificateException("Signer certificate not found in signature");
        }

        X509CertificateHolder certHolder = certCollection.iterator().next();
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }
}
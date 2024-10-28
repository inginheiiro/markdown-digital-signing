package com.md.sign;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markdown")
public class SignatureController {

    private final DigitalSignatureService signatureService;

    @Autowired
    public SignatureController(DigitalSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    /**
     * Signs a markdown document with optional metadata.
     *
     * @param markdown The markdown content to sign
     * @param metadata Optional metadata to include in the signature
     * @return The signed markdown document
     */
    @PostMapping(value = "/sign",
            consumes = MediaType.TEXT_MARKDOWN_VALUE,
            produces = MediaType.TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> signMarkdown(
            @RequestBody String markdown,
            @RequestParam(required = false) Map<String, String> metadata) {
        try {
            String signedMarkdown = signatureService.signMarkdown(markdown, metadata);
            return ResponseEntity.ok(signedMarkdown);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to sign document: " + e.getMessage());
        }
    }

    /**
     * Verifies all signatures in a markdown document.
     *
     * @param signedMarkdown The signed markdown document to verify
     * @return List of verification results for each signature
     */
    @PostMapping(value = "/verify",
            consumes = MediaType.TEXT_MARKDOWN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SignatureVerificationResult>> verifySignatures(
            @RequestBody String signedMarkdown) {
        try {
            List<SignatureVerificationResult> results = signatureService.verifySignatures(signedMarkdown);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of(
                    new SignatureVerificationResult(
                            false,
                            null,
                            "Verification failed: " + e.getMessage()
                    )
            ));
        }
    }

    /**
     * Handles exceptions thrown by the signature service.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.badRequest()
                .body("Error processing request: " + e.getMessage());
    }
}
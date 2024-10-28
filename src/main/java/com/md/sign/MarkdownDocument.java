package com.md.sign;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class MarkdownDocument {
    private Map<String, Object> frontMatter;
    private String content;
    private final List<DocumentSignature> signatures;

    public MarkdownDocument() {
        this.frontMatter = new HashMap<>();
        this.signatures = new ArrayList<>();
    }

    public Map<String, Object> getFrontMatter() { return frontMatter; }
    public void setFrontMatter(Map<String, Object> frontMatter) {
        this.frontMatter = frontMatter;
    }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<DocumentSignature> getSignatures() { return signatures; }

    public void addSignature(DocumentSignature signature) {
        this.signatures.add(signature);
        updateFrontMatter();
    }

    private void updateFrontMatter() {
        List<Map<String, Object>> signatureData = new ArrayList<>();
        for (DocumentSignature sig : signatures) {
            Map<String, Object> sigMap = new HashMap<>();
            sigMap.put("signature", sig.signature());
            sigMap.put("signerDN", sig.signerDN());
            sigMap.put("expirationDate", sig.expirationDate().toString());
            sigMap.put("signedAt", sig.signedAt().toString());
            sigMap.put("metadata", sig.metadata());
            signatureData.add(sigMap);
        }
        frontMatter.put("signatures", signatureData);
    }
}
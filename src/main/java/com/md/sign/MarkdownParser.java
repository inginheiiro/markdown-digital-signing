package com.md.sign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownParser {
    private static final Logger logger = LoggerFactory.getLogger(MarkdownParser.class);
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\r?\\n(.*?)\\r?\\n---\\r?\\n(.*)$", Pattern.DOTALL);

    @SuppressWarnings("unchecked")
    public static MarkdownDocument parse(String markdown) {
        MarkdownDocument doc = new MarkdownDocument();

        try {
            if (markdown == null || markdown.trim().isEmpty()) {
                logger.warn("Empty markdown content");
                return doc;
            }

            logger.debug("Parsing markdown content of length: {}", markdown.length());
            Matcher matcher = FRONT_MATTER_PATTERN.matcher(markdown);

            if (matcher.find()) {
                String yamlString = matcher.group(1);
                String content = matcher.group(2);

                logger.debug("Found YAML front matter: {}", yamlString);

                LoaderOptions options = new LoaderOptions();
                options.setAllowDuplicateKeys(false);
                Yaml yaml = new Yaml(options);
                Map<String, Object> frontMatter = yaml.load(yamlString);

                if (frontMatter != null) {
                    // Parse signatures section
                    if (frontMatter.containsKey("signatures")) {
                        Object signaturesObj = frontMatter.get("signatures");
                        if (signaturesObj instanceof List) {
                            List<Map<String, Object>> signatures = (List<Map<String, Object>>) signaturesObj;
                            for (Map<String, Object> sigMap : signatures) {
                                parseAndAddSignature(doc, sigMap);
                            }
                        }
                    }

                    doc.setFrontMatter(frontMatter);
                } else {
                    doc.setFrontMatter(new HashMap<>());
                }

                doc.setContent(content.trim());
            } else {
                logger.debug("No front matter found, treating entire content as markdown");
                doc.setContent(markdown.trim());
            }

        } catch (Exception e) {
            logger.error("Error parsing markdown document: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to parse markdown document: " + e.getMessage(), e);
        }

        return doc;
    }

    @SuppressWarnings("unchecked")
    private static void parseAndAddSignature(MarkdownDocument doc, Map<String, Object> sigMap) {
        try {
            String signature = (String) sigMap.get("signature");
            String signerDN = (String) sigMap.get("signerDN");

            // Parse dates
            Instant signedAt = parseInstant(sigMap.get("signedAt"));
            Instant expirationDate = parseInstant(sigMap.get("expirationDate"));

            // Parse metadata
            Map<String, String> metadata = new HashMap<>();
            Object metadataObj = sigMap.get("metadata");
            if (metadataObj instanceof Map) {
                ((Map<String, Object>) metadataObj).forEach((key, value) ->
                        metadata.put(key, value != null ? value.toString() : null));
            }

            DocumentSignature documentSignature = new DocumentSignature(
                    signature,
                    signerDN,
                    expirationDate,
                    metadata,
                    signedAt
            );

            doc.addSignature(documentSignature);
            logger.debug("Added signature for DN: {}", signerDN);

        } catch (Exception e) {
            logger.error("Error parsing signature data: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to parse signature data: " + e.getMessage(), e);
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                return Instant.parse((String) value);
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed to parse instant value: {}", value);
            return null;
        }
    }

    public static String serialize(MarkdownDocument doc) {
        StringBuilder sb = new StringBuilder();

        try {
            Map<String, Object> frontMatter = new HashMap<>(doc.getFrontMatter());

            // Prepare signatures data
            if (!doc.getSignatures().isEmpty()) {
                List<Map<String, Object>> signaturesData = new ArrayList<>();

                for (DocumentSignature sig : doc.getSignatures()) {
                    Map<String, Object> sigMap = new HashMap<>();
                    sigMap.put("signature", sig.signature());
                    sigMap.put("signerDN", sig.signerDN());

                    if (sig.expirationDate() != null) {
                        sigMap.put("expirationDate", sig.expirationDate().toString());
                    }
                    if (sig.signedAt() != null) {
                        sigMap.put("signedAt", sig.signedAt().toString());
                    }
                    if (!sig.metadata().isEmpty()) {
                        sigMap.put("metadata", sig.metadata());
                    }

                    signaturesData.add(sigMap);
                }

                frontMatter.put("signatures", signaturesData);
            }

            // Add front matter if not empty
            if (!frontMatter.isEmpty()) {
                sb.append("---\n");
                Yaml yaml = new Yaml();
                sb.append(yaml.dump(frontMatter));
                sb.append("---\n\n");
            }

            // Add content
            sb.append(doc.getContent());
            if (!doc.getContent().endsWith("\n")) {
                sb.append("\n");
            }

        } catch (Exception e) {
            logger.error("Error serializing markdown document: {}", e.getMessage());
            throw new IllegalStateException("Failed to serialize markdown document: " + e.getMessage(), e);
        }

        return sb.toString();
    }
}
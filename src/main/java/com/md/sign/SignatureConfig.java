package com.md.sign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Enumeration;

@Configuration
public class SignatureConfig {

    private static final Logger logger = LoggerFactory.getLogger(SignatureConfig.class);

    @Value("${signature.keystore.path}")
    private String keystorePath;

    @Value("${signature.keystore.password}")
    private String keystorePassword;

    @Value("${signature.keystore.alias}")
    private String keystoreAlias;

    @Bean
    public KeyStore keyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        Resource resource = new ClassPathResource(keystorePath);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Keystore file not found: " + keystorePath
            );
        }

        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
            logger.info("Successfully loaded keystore from: {}", keystorePath);

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                logger.info("Found alias in keystore: {}", alias);
            }

            if (!keyStore.containsAlias(keystoreAlias)) {
                throw new IllegalStateException(
                        "Keystore does not contain required alias: " + keystoreAlias +
                                ". Please check your keystore configuration."
                );
            }

            return keyStore;
        }
    }

    @Bean
    public String keystorePassword() {
        return keystorePassword;
    }

    @Bean
    public String keystoreAlias() {
        return keystoreAlias;
    }
}
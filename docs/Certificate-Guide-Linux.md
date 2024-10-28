# Linux-Guide.md
```markdown
# Digital Signature Setup for Linux

## 1. Create Project Structure
```bash
mkdir -p src/main/resources/certificates
cd src/main/resources/certificates
```

## 2. Generate Keystore and Certificate
```bash
keytool -genkeypair \
    -alias markdown-sign \
    -keyalg RSA \
    -keysize 2048 \
    -keystore keystore.p12 \
    -storetype PKCS12 \
    -storepass changeit \
    -validity 365 \
    -dname "CN=markdown-sign, OU=Development, O=MarkdownSign, L=Lisboa, ST=Lisboa, C=PT" \
    -ext KeyUsage=digitalSignature \
    -ext ExtendedKeyUsage=codeSigning
```

## 3. Verify Keystore
```bash
keytool -list -v -keystore keystore.p12 -storepass changeit
```

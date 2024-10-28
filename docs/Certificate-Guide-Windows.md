# Windows-Guide.md

```markdown
# Digital Signature Setup for Windows

## 1. Create Project Structure
```cmd
mkdir src\main\resources\certificates
cd src\main\resources\certificates
```

## 2. Generate Keystore and Certificate
```cmd
keytool -genkeypair -alias markdown-sign -keyalg RSA -keysize 2048 -keystore keystore.p12 -storetype PKCS12 -storepass changeit -validity 365 -dname "CN=markdown-sign, OU=Development, O=MarkdownSign, L=Lisboa, ST=Lisboa, C=PT" -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=codeSigning
```

## 3. Verify Keystore
```cmd
keytool -list -v -keystore keystore.p12 -storepass changeit
```

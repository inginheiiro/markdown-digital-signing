# Windows-Markdown-Guide.md

```markdown
# Digital Signature Guide for Windows

## Prerequisites
- Running Spring Boot application on http://localhost:8080
- curl installed on Windows
- A test document file

## Create Test Document
Create a file named `test.md` with this content:

```markdown
# Test Document

This is a test document that will be signed multiple times.

## Content
This document requires multiple signatures for approval.

## Requirements
- Engineering Review
- Technical Review
- Management Approval
```

## Basic Commands

### 1. Single Signature
```bash
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=John%%20Doe&metadata.department=Engineering" -H "Content-Type: text/markdown" --data-binary "@test.md" -o signed.md
```

### 2. Verify a Signature
```bash
curl -X POST "http://localhost:8080/api/markdown/verify" -H "Content-Type: text/markdown" --data-binary "@signed.md"
```

### 3. Sign with Multiple Metadata Fields
```bash
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=John%%20Doe&metadata.department=Engineering&metadata.date=2024-10-28" -H "Content-Type: text/markdown" --data-binary "@test.md" -o signed.md
```

## Multiple Signatures Example

### 1. Engineering Signature
```bash
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=John%%20Doe&metadata.role=Engineer&metadata.department=Engineering" -H "Content-Type: text/markdown" --data-binary "@test.md" -o signed_1.md
```

### 2. Technical Review Signature
```bash
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=Jane%%20Smith&metadata.role=Reviewer" -H "Content-Type: text/markdown" --data-binary "@signed_1.md" -o signed_2.md
```

### 3. Management Approval Signature
```bash
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=Bob%%20Manager&metadata.status=Approved" -H "Content-Type: text/markdown" --data-binary "@signed_2.md" -o signed_final.md
```

### 4. Verify All Signatures
```bash
curl -X POST "http://localhost:8080/api/markdown/verify" -H "Content-Type: text/markdown" --data-binary "@signed_final.md"
```

## Debug Commands

### Verbose Output
```bash
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=John%%20Doe&metadata.department=Engineering" -H "Content-Type: text/markdown" --data-binary "@test.md" -v -o signed.md
```

### View Signed Document
```bash
type signed.md
type signed_1.md
type signed_2.md
type signed_final.md
```

## Important Notes for Windows
1. Use `%%20` for spaces in URL parameters
2. Use `@` before file names for file input
3. Use quotes around URLs with parameters
4. File paths are relative to current directory
5. Forward slashes work in paths
6. Each signature includes timestamp, expiration, metadata, and cryptographic signature

## Complete Working Example
```bash
REM Create test document
echo # Test Document > test.md
echo This is a test document >> test.md

REM First signature
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=John%%20Doe&metadata.department=Engineering" -H "Content-Type: text/markdown" --data-binary "@test.md" -o signed_1.md

REM Second signature
curl -X POST "http://localhost:8080/api/markdown/sign?metadata.author=Jane%%20Smith&metadata.role=Reviewer" -H "Content-Type: text/markdown" --data-binary "@signed_1.md" -o signed_2.md

REM Verify signatures
curl -X POST "http://localhost:8080/api/markdown/verify" -H "Content-Type: text/markdown" --data-binary "@signed_2.md"
```
```


# Linux-Markdown-Guide.md

```markdown
# Digital Signature Guide for Linux/Unix

## Prerequisites
- Running Spring Boot application on http://localhost:8080
- curl installed
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
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@test.md" \
  -G \
  --data-urlencode "metadata.author=John Doe" \
  --data-urlencode "metadata.department=Engineering" \
  -o signed.md
```

### 2. Verify a Signature
```bash
curl -X POST 'http://localhost:8080/api/markdown/verify' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@signed.md"
```

### 3. Sign with Multiple Metadata Fields
```bash
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@test.md" \
  -G \
  --data-urlencode "metadata.author=John Doe" \
  --data-urlencode "metadata.department=Engineering" \
  --data-urlencode "metadata.date=$(date +%Y-%m-%d)" \
  -o signed.md
```

## Multiple Signatures Example

### 1. Engineering Signature
```bash
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@test.md" \
  -G \
  --data-urlencode "metadata.author=John Doe" \
  --data-urlencode "metadata.role=Engineer" \
  --data-urlencode "metadata.department=Engineering" \
  -o signed_1.md
```

### 2. Technical Review Signature
```bash
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@signed_1.md" \
  -G \
  --data-urlencode "metadata.author=Jane Smith" \
  --data-urlencode "metadata.role=Reviewer" \
  -o signed_2.md
```

### 3. Management Approval Signature
```bash
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@signed_2.md" \
  -G \
  --data-urlencode "metadata.author=Bob Manager" \
  --data-urlencode "metadata.status=Approved" \
  -o signed_final.md
```

### 4. Verify All Signatures
```bash
curl -X POST 'http://localhost:8080/api/markdown/verify' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@signed_final.md"
```

## Debug Commands

### Verbose Output
```bash
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@test.md" \
  -G \
  --data-urlencode "metadata.author=John Doe" \
  --data-urlencode "metadata.department=Engineering" \
  -v \
  -o signed.md
```

### View Signed Document
```bash
cat signed.md
cat signed_1.md
cat signed_2.md
cat signed_final.md
```

## Important Notes for Linux
1. Use `-G` with `--data-urlencode` for proper URL encoding
2. Use single quotes around URLs
3. Use backslashes for line continuation
4. Use `@` before file names for file input
5. File paths are relative to current directory
6. Each signature includes timestamp, expiration, metadata, and cryptographic signature

## Complete Working Example
```bash
# Create test document
cat > test.md << 'EOF'
# Test Document
This is a test document
EOF

# First signature
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@test.md" \
  -G \
  --data-urlencode "metadata.author=John Doe" \
  --data-urlencode "metadata.department=Engineering" \
  -o signed_1.md

# Second signature
curl -X POST 'http://localhost:8080/api/markdown/sign' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@signed_1.md" \
  -G \
  --data-urlencode "metadata.author=Jane Smith" \
  --data-urlencode "metadata.role=Reviewer" \
  -o signed_2.md

# Verify signatures
curl -X POST 'http://localhost:8080/api/markdown/verify' \
  -H 'Content-Type: text/markdown' \
  --data-binary "@signed_2.md"
```
```


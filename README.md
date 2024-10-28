# Digital Signature System for Markdown Documents

A Spring Boot application that provides digital signature capabilities for Markdown documents, supporting multiple signatures with metadata and verification.

## Documentation

### Setup Guides
- [Windows Setup Guide](docs/Windows-Markdown-Guide.md)
    - Windows-specific commands and examples
    - Certificate generation for Windows
    - Testing procedures

- [Linux Setup Guide](docs/Linux-Markdown-Guide.md)
    - Linux-specific commands and examples
    - Certificate generation for Linux
    - Testing procedures

### Certificate Configuration Windows
- [Certificate Setup Guide](docs/Certificate-Guide-Windows.md)
    - How to generate certificates
    - 
### Certificate Configuration Linux
- [Certificate Setup Guide](docs/Certificate-Guide-Linux.md)
    - How to generate certificates


### API Documentation
The system provides REST endpoints for:
- Signing markdown documents with metadata
- Adding multiple signatures
- Verifying document signatures

## Features
- Multiple signature support
- Custom metadata per signature
- Signature verification
- Certificate validation
- Timestamping
- Digital signature expiration

## Usage Examples
See the platform-specific guides for detailed examples of:
- Single signature creation
- Multiple signature chains
- Signature verification
- Debug commands

## Technical Details
- Built with Spring Boot
- Uses Bouncy Castle for cryptographic operations
- PKCS#12 keystore format
- SHA256withRSA signatures
- X.509 certificates

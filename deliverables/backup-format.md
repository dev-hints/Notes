# Backup Specification & Cryptographic Format — Notes

## 1. Specification Overview

**Notes** (branded as **Notes by StrangeInfinity**) implements an offline, portable, versioned JSON backup format. Backups are self-contained files that encapsulate all user notes, rich text attributes, checklists, vector drawings, folders, tags, and file attachments.

To guarantee complete user privacy and data security against unauthorized access or device seizure, backups support authenticated symmetric encryption using industry-standard **AES-256-GCM** with **PBKDF2-HMAC-SHA256** key stretching.

---

## 2. Cryptographic Architecture

### 2.1. Cryptographic Primitives

| Parameter | Specification | Purpose |
| :--- | :--- | :--- |
| **Cipher** | `AES/GCM/NoPadding` | Authenticated Encryption with Associated Data (AEAD) |
| **Key Size** | 256 bits (32 bytes) | Resistance to brute-force attacks |
| **Authentication Tag** | 128 bits (16 bytes) | Cryptographic tamper detection and ciphertext integrity |
| **Nonce / IV** | 96 bits (12 bytes) | Standard GCM initialization vector generated via `SecureRandom` |
| **Key Derivation** | `PBKDF2WithHmacSHA256` | Password-based key stretching |
| **Salt** | 128 bits (16 bytes) | Cryptographically secure random salt to thwart rainbow tables |
| **KDF Iterations** | 100,000 iterations | High computational cost to resist GPU/ASIC offline attacks |
| **Associated Data (AAD)** | `strangeinfinity.notes.backup.v1` | Binds ciphertext to the specific backup version and schema |

### 2.2. Threat Model & Security Guarantees
1. **Confidentiality**: Even if a backup file is stored on an untrusted cloud drive, flash drive, or inspected by third-party applications, the underlying note contents, drawing strokes, titles, and attachments cannot be read without the user-defined passphrase.
2. **Integrity & Tamper Detection**: AES-GCM's 128-bit authentication tag ensures that any modification to the ciphertext, salt, nonce, or format metadata triggers an immediate `AEADBadTagException` (mapped to `InvalidBackupCredentialsOrCorruptedException`), rejecting corrupted or maliciously altered archives before any database operations occur.
3. **No Key Storage**: The encryption key is never persisted on disk, in DataStore, or in Room. Keys are derived ephemerally in RAM during export/import and immediately wiped from memory.

---

## 3. Envelope Specification

The backup is serialized as a single UTF-8 encoded JSON envelope with top-level format identification and schema versioning.

### 3.1. Envelope Schema

```json
{
  "format": "strangeinfinity.notes.backup",
  "formatVersion": 1,
  "app": "Notes by StrangeInfinity",
  "encryption": {
    "algorithm": "AES-256-GCM",
    "kdf": "PBKDF2-HMAC-SHA256",
    "iterations": 100000,
    "salt": "<Base64 16-byte salt>",
    "nonce": "<Base64 12-byte nonce>"
  },
  "ciphertext": "<Base64 ciphertext + 16-byte auth tag>",
  "unencrypted": false,
  "plainPayload": null
}
```

### 3.2. Unencrypted Backup Envelope (Fallback Mode)
When the user explicitly opts out of password protection, the `encryption` and `ciphertext` fields are `null`, and `plainPayload` contains the unencrypted JSON payload:

```json
{
  "format": "strangeinfinity.notes.backup",
  "formatVersion": 1,
  "app": "Notes by StrangeInfinity",
  "encryption": null,
  "ciphertext": null,
  "unencrypted": true,
  "plainPayload": { ... }
}
```

---

## 4. Inner Payload Schema (`BackupPayload`)

The decrypted payload (or plaintext payload) contains the complete relational snapshot:

```json
{
  "schemaVersion": 1,
  "app": "Notes by StrangeInfinity",
  "exportedAt": "2026-10-23T07:29:43Z",
  "notes": [
    {
      "id": "e4c9f130-f8c6-4d57-9d62-1b12b591b384",
      "title": "Project Roadmap & Strategic Goals",
      "content": "Deliver milestone 1 with 100% offline verification.",
      "folderId": "f1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "isPinned": true,
      "isArchived": false,
      "isDeleted": false,
      "color": 4293914607,
      "hasChecklist": true,
      "checklistData": "[{\"id\":\"c1\",\"text\":\"Review PRs\",\"isDone\":true,\"order\":0}]",
      "hasDrawing": false,
      "drawingData": null,
      "createdAt": 1792740000000,
      "updatedAt": 1792740500000
    }
  ],
  "folders": [
    {
      "id": "f1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "name": "Engineering",
      "color": 4284918783,
      "createdAt": 1792739000000
    }
  ],
  "tags": [
    {
      "id": "t1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "name": "Roadmap",
      "createdAt": 1792739100000
    }
  ],
  "noteTagRefs": [
    {
      "noteId": "e4c9f130-f8c6-4d57-9d62-1b12b591b384",
      "tagId": "t1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c"
    }
  ],
  "attachments": [
    {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "noteId": "e4c9f130-f8c6-4d57-9d62-1b12b591b384",
      "fileName": "diagram.png",
      "mimeType": "image/png",
      "fileSize": 45120,
      "createdAt": 1792740200000,
      "dataBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
    }
  ]
}
```

---

## 5. Import & Restoration Workflow

### 5.1. Safety Previews
Before modifying the local database, `BackupManager.previewBackup()` decodes the envelope, performs password verification, and extracts high-level metadata:
- Export timestamp
- Note count
- Folder count
- Tag count
- Attachment count
- Encryption status

The user reviews this metadata in an interactive confirmation dialog before committing to a restore.

### 5.2. Atomic Transaction Guarantee
Restoration is executed inside a single Room database transaction (`database.withTransaction`):
- **Mode: REPLACE (Clean Restore)**:
  1. Clears existing database tables (`attachments`, `note_tags`, `notes`, `tags`, `folders`).
  2. Inserts all entities from the backup.
  3. Writes decoded attachment binaries to application-sandboxed storage (`files/attachments/`).
- **Mode: MERGE (Additive Restore)**:
  1. Inserts or updates entities using conflict resolution strategy (`OnConflictStrategy.REPLACE`).
  2. Preserves existing notes that are not in the backup.
  3. Recreates missing folder associations and tags.
- **Rollback Guarantee**: If any failure occurs during decryption, JSON parsing, disk write, or SQL insertion, the transaction rolls back completely. The user's active database is left 100% unaltered.

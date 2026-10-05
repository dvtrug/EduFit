package vn.edufit.verification.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity đại diện cho bảng {@code credential_file}.
 */
@Entity
@Table(name = "credential_file")
public class CredentialFile {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "file_id", updatable = false, nullable = false)
  private UUID fileId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credential_id", nullable = false)
  private Credential credential;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private Instant uploadedAt = Instant.now();

  protected CredentialFile() {
    // JPA required
  }

  public CredentialFile(
      String fileName,
      String storageKey,
      String contentType,
      Long sizeBytes,
      Instant uploadedAt
  ) {
    this.fileName = fileName;
    this.storageKey = storageKey;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.uploadedAt = uploadedAt != null ? uploadedAt : Instant.now();
  }

  public static CredentialFile create(
      String fileName,
      String storageKey,
      String contentType,
      Long sizeBytes,
      Instant uploadedAt
  ) {
    return new CredentialFile(fileName, storageKey, contentType, sizeBytes, uploadedAt);
  }

  void assignCredential(Credential credential) {
    this.credential = credential;
  }

  // Getters
  public UUID getFileId() {
    return fileId;
  }

  public Credential getCredential() {
    return credential;
  }

  public String getFileName() {
    return fileName;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }
}


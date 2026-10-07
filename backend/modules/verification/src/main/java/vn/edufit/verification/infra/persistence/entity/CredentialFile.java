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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credential_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CredentialFile {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "file_id")
  private UUID fileId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credential_id", nullable = false)
  private Credential credential;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private Instant uploadedAt;

  public static CredentialFile create(
      String fileName,
      String storageKey,
      String contentType,
      long sizeBytes,
      Instant uploadedAt
  ) {
    CredentialFile file = new CredentialFile();
    file.fileName = fileName;
    file.storageKey = storageKey;
    file.contentType = contentType;
    file.sizeBytes = sizeBytes;
    file.uploadedAt = uploadedAt;
    return file;
  }

  void attachTo(Credential credential) {
    this.credential = credential;
  }
}

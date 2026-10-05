package vn.edufit.verification.infra.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity đại diện cho bảng {@code credential}.
 */
@Entity
@Table(name = "credential")
public class Credential {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "credential_id", updatable = false, nullable = false)
  private UUID credentialId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false)
  private VerificationRequest request;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private CredentialType type;

  @Column(name = "institution", nullable = false, length = 200)
  private String institution;

  @Column(name = "year")
  private Short year;

  @Column(name = "note", length = 500)
  private String note;

  @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CredentialFile> files = new ArrayList<>();

  protected Credential() {
    // JPA required
  }

  public Credential(CredentialType type, String institution, Short year, String note) {
    this.type = type;
    this.institution = institution;
    this.year = year;
    this.note = note;
  }

  public static Credential create(CredentialType type, String institution, Short year, String note) {
    return new Credential(type, institution, year, note);
  }

  void assignRequest(VerificationRequest request) {
    this.request = request;
  }

  public void addFile(CredentialFile file) {
    if (file != null) {
      this.files.add(file);
      file.assignCredential(this);
    }
  }

  // Getters
  public UUID getCredentialId() {
    return credentialId;
  }

  public VerificationRequest getRequest() {
    return request;
  }

  public CredentialType getType() {
    return type;
  }

  public String getInstitution() {
    return institution;
  }

  public Short getYear() {
    return year;
  }

  public String getNote() {
    return note;
  }

  public List<CredentialFile> getFiles() {
    return Collections.unmodifiableList(files);
  }
}


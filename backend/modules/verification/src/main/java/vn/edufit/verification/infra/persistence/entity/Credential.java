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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credential")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credential {

  public enum Type {
    DEGREE,
    CERTIFICATE,
    TEACHING_LICENCE,
    STUDENT_CARD,
    OTHER
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "credential_id")
  private UUID credentialId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private VerificationRequest verificationRequest;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private Type type;

  @Column(name = "institution", nullable = false, length = 200)
  private String institution;

  @Column(name = "year")
  private Short year;

  @Column(name = "note", length = 500)
  private String note;

  @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("uploadedAt ASC")
  private List<CredentialFile> files = new ArrayList<>();

  public static Credential create(Type type, String institution, Short year, String note) {
    Credential credential = new Credential();
    credential.type = type;
    credential.institution = institution.trim();
    credential.year = year;
    credential.note = note == null || note.isBlank() ? null : note.trim();
    return credential;
  }

  void attachTo(VerificationRequest request) {
    this.verificationRequest = request;
  }

  public void addFile(CredentialFile file) {
    files.add(file);
    file.attachTo(this);
  }
}

package vn.edufit.verification.web.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.edufit.verification.infra.persistence.entity.Credential;
import vn.edufit.verification.infra.persistence.entity.CredentialFile;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;

public record VerificationRequestDetailResponse(
    UUID requestId,
    UUID tutorId,
    String status,
    String rejectionReason,
    Instant submittedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    List<CredentialResponse> credentials
) {

  public record CredentialResponse(
      UUID credentialId,
      String type,
      String institution,
      Short year,
      String note,
      List<CredentialFileResponse> files
  ) {}

  public record CredentialFileResponse(
      UUID fileId,
      String fileName,
      String storageKey,
      String contentType,
      Long sizeBytes,
      Instant uploadedAt
  ) {}

  public static VerificationRequestDetailResponse from(VerificationRequest request) {
    List<CredentialResponse> credentialResponses = request.getCredentials().stream()
        .map(VerificationRequestDetailResponse::mapCredential)
        .toList();

    return new VerificationRequestDetailResponse(
        request.getRequestId(),
        request.getTutorId(),
        request.getStatus().name(),
        request.getRejectionReason(),
        request.getSubmittedAt(),
        request.getReviewedBy(),
        request.getReviewedAt(),
        credentialResponses
    );
  }

  private static CredentialResponse mapCredential(Credential cred) {
    List<CredentialFileResponse> fileResponses = cred.getFiles().stream()
        .map(VerificationRequestDetailResponse::mapFile)
        .toList();

    return new CredentialResponse(
        cred.getCredentialId(),
        cred.getType().name(),
        cred.getInstitution(),
        cred.getYear(),
        cred.getNote(),
        fileResponses
    );
  }

  private static CredentialFileResponse mapFile(CredentialFile file) {
    return new CredentialFileResponse(
        file.getFileId(),
        file.getFileName(),
        file.getStorageKey(),
        file.getContentType(),
        file.getSizeBytes(),
        file.getUploadedAt()
    );
  }
}


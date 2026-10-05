package vn.edufit.verification.application.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;
import vn.edufit.storage.api.CredentialStorageService;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.verification.infra.persistence.entity.Credential;
import vn.edufit.verification.infra.persistence.entity.CredentialFile;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;
import vn.edufit.verification.infra.persistence.repository.VerificationRequestRepository;
import vn.edufit.verification.web.request.SubmitVerificationRequest;
import vn.edufit.verification.web.response.VerificationRequestDetailResponse;

@Service
public class TutorVerificationService {

  private final VerificationRequestRepository verificationRequestRepository;
  private final CredentialStorageService credentialStorageService;
  private final EntityManager entityManager;

  public TutorVerificationService(
      VerificationRequestRepository verificationRequestRepository,
      CredentialStorageService credentialStorageService,
      EntityManager entityManager
  ) {
    this.verificationRequestRepository = verificationRequestRepository;
    this.credentialStorageService = credentialStorageService;
    this.entityManager = entityManager;
  }

  @Transactional
  public VerificationRequestDetailResponse submit(
      CurrentUser currentUser,
      SubmitVerificationRequest request,
      List<MultipartFile> files
  ) {
    ensureTutor(currentUser);
    UUID tutorId = findTutorIdByUserId(currentUser.getUserId());
    if (verificationRequestRepository.existsByTutorIdAndStatus(tutorId, VerificationRequest.Status.PENDING)) {
      throw new ResourceConflictException(
          ErrorCode.CONFLICT_DETECTED,
          "Gia sư đã có hồ sơ xác minh đang chờ duyệt"
      );
    }
    if (files == null || files.isEmpty()) {
      throw new InvalidOperationException(
          ErrorCode.CREDENTIAL_NOT_SUBMITTED,
          "Cần tải lên ít nhất một file bằng cấp"
      );
    }

    List<String> storedKeys = new ArrayList<>();
    try {
      VerificationRequest verificationRequest = VerificationRequest.pending(tutorId, Instant.now());
      for (SubmitVerificationRequest.CredentialItem item : request.credentials()) {
        Credential credential = Credential.create(item.type(), item.institution(), item.year(), item.note());
        for (Integer fileIndex : item.fileIndexes()) {
          MultipartFile file = getFile(files, fileIndex);
          StoredCredentialFile storedFile = credentialStorageService.store(file);
          storedKeys.add(storedFile.storageKey());
          credential.addFile(CredentialFile.create(
              storedFile.fileName(),
              storedFile.storageKey(),
              storedFile.contentType(),
              storedFile.sizeBytes(),
              Instant.now()
          ));
        }
        verificationRequest.addCredential(credential);
      }

      VerificationRequest saved = verificationRequestRepository.save(verificationRequest);
      markTutorPendingVerification(tutorId);
      return VerificationRequestDetailResponse.from(saved);
    } catch (RuntimeException ex) {
      storedKeys.forEach(credentialStorageService::delete);
      throw ex;
    }
  }

  @Transactional(readOnly = true)
  public VerificationRequestDetailResponse getMyLatestRequest(CurrentUser currentUser) {
    ensureTutor(currentUser);
    UUID tutorId = findTutorIdByUserId(currentUser.getUserId());
    VerificationRequest request = verificationRequestRepository
        .findWithCredentialsByRequestId(
            verificationRequestRepository.findFirstByTutorIdOrderBySubmittedAtDesc(tutorId)
                .orElseThrow(() -> new EntityNotFoundException("Gia sư chưa có hồ sơ xác minh"))
                .getRequestId()
        )
        .orElseThrow(() -> new EntityNotFoundException("Gia sư chưa có hồ sơ xác minh"));
    return VerificationRequestDetailResponse.from(request);
  }

  private MultipartFile getFile(List<MultipartFile> files, Integer fileIndex) {
    if (fileIndex == null || fileIndex < 0 || fileIndex >= files.size()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Chỉ mục file bằng cấp không hợp lệ"
      );
    }
    return files.get(fileIndex);
  }

  private void ensureTutor(CurrentUser currentUser) {
    if (currentUser == null || !currentUser.hasRole("TUTOR")) {
      throw new InvalidOperationException(
          ErrorCode.FORBIDDEN,
          "Chỉ tài khoản gia sư mới được nộp hồ sơ xác minh"
      );
    }
  }

  private UUID findTutorIdByUserId(UUID userId) {
    List<?> results = entityManager
        .createNativeQuery("SELECT tutor_id FROM tutor_profile WHERE user_id = :userId")
        .setParameter("userId", userId)
        .getResultList();
    if (results.isEmpty()) {
      throw new EntityNotFoundException("Không tìm thấy hồ sơ gia sư");
    }
    Object result = results.getFirst();
    return result instanceof UUID tutorId ? tutorId : UUID.fromString(result.toString());
  }

  private void markTutorPendingVerification(UUID tutorId) {
    entityManager
        .createNativeQuery("""
            UPDATE tutor_profile
            SET status = 'PENDING_VERIFICATION', updated_at = NOW()
            WHERE tutor_id = :tutorId
            """)
        .setParameter("tutorId", tutorId)
        .executeUpdate();
  }
}

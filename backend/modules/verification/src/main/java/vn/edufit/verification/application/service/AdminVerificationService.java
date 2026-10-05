package vn.edufit.verification.application.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;
import vn.edufit.verification.infra.persistence.repository.VerificationRequestRepository;
import vn.edufit.verification.web.request.ReviewVerificationRequest;
import vn.edufit.verification.web.response.PendingVerificationQueueResponse;
import vn.edufit.verification.web.response.VerificationRequestDetailResponse;

@Service
public class AdminVerificationService {

  private final VerificationRequestRepository verificationRequestRepository;
  private final EntityManager entityManager;

  public AdminVerificationService(
      VerificationRequestRepository verificationRequestRepository,
      EntityManager entityManager
  ) {
    this.verificationRequestRepository = verificationRequestRepository;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public PendingVerificationQueueResponse getPendingQueue(CurrentUser currentUser, int page, int size) {
    ensureAdmin(currentUser);
    Page<VerificationRequest> pendingPage = verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(
        VerificationRequest.Status.PENDING,
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
    );
    return PendingVerificationQueueResponse.from(pendingPage);
  }

  @Transactional
  public VerificationRequestDetailResponse review(
      CurrentUser currentUser,
      UUID requestId,
      ReviewVerificationRequest reviewRequest
  ) {
    ensureAdmin(currentUser);
    VerificationRequest request = verificationRequestRepository.findWithCredentialsByRequestId(requestId)
        .orElseThrow(() -> EntityNotFoundException.of("VerificationRequest", requestId));

    if (reviewRequest.action() == ReviewVerificationRequest.Action.APPROVE) {
      request.approve(currentUser.getUserId(), Instant.now());
      markTutorVerified(request.getTutorId());
    } else {
      request.reject(currentUser.getUserId(), reviewRequest.reason(), Instant.now());
      markTutorRejected(request.getTutorId());
    }

    return VerificationRequestDetailResponse.from(request);
  }

  private void ensureAdmin(CurrentUser currentUser) {
    if (currentUser == null || !currentUser.hasRole("ADMIN")) {
      throw new InvalidOperationException(
          ErrorCode.FORBIDDEN,
          "Chỉ quản trị viên mới được duyệt hồ sơ xác minh"
      );
    }
  }

  private void markTutorVerified(UUID tutorId) {
    entityManager
        .createNativeQuery("""
            UPDATE tutor_profile
            SET status = 'VERIFIED', verified_at = NOW(), updated_at = NOW()
            WHERE tutor_id = :tutorId
            """)
        .setParameter("tutorId", tutorId)
        .executeUpdate();
  }

  private void markTutorRejected(UUID tutorId) {
    entityManager
        .createNativeQuery("""
            UPDATE tutor_profile
            SET status = 'UNVERIFIED', verified_at = NULL, updated_at = NOW()
            WHERE tutor_id = :tutorId
            """)
        .setParameter("tutorId", tutorId)
        .executeUpdate();
  }
}

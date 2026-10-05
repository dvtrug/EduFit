package vn.edufit.verification.application.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.ProfileFacade;
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
  private final ProfileFacade profileFacade;

  public AdminVerificationService(
      VerificationRequestRepository verificationRequestRepository,
      ProfileFacade profileFacade
  ) {
    this.verificationRequestRepository = verificationRequestRepository;
    this.profileFacade = profileFacade;
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

    if (request.getStatus() != VerificationRequest.Status.PENDING) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_OPERATION,
          "Hồ sơ xác minh này đã được xử lý trước đó"
      );
    }

    if (reviewRequest.action() == ReviewVerificationRequest.Action.APPROVE) {
      Instant now = Instant.now();
      request.approve(currentUser.getUserId(), now);
      profileFacade.markTutorVerified(request.getTutorId(), now);
    } else {
      if (reviewRequest.reason() == null || reviewRequest.reason().isBlank()) {
        throw new InvalidOperationException(
            ErrorCode.VALIDATION_FAILED,
            "Cần cung cấp lý do từ chối hồ sơ xác minh"
        );
      }
      request.reject(currentUser.getUserId(), reviewRequest.reason().trim(), Instant.now());
      profileFacade.markTutorRejected(request.getTutorId());
    }

    VerificationRequest saved = verificationRequestRepository.save(request);
    return VerificationRequestDetailResponse.from(saved);
  }

  private void ensureAdmin(CurrentUser currentUser) {
    if (currentUser == null || !currentUser.hasRole("ADMIN")) {
      throw new InvalidOperationException(
          ErrorCode.FORBIDDEN,
          "Chỉ quản trị viên mới được duyệt hồ sơ xác minh"
      );
    }
  }
}


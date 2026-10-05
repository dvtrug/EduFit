package vn.edufit.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.verification.application.service.AdminVerificationService;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;
import vn.edufit.verification.infra.persistence.repository.VerificationRequestRepository;
import vn.edufit.verification.web.request.ReviewVerificationRequest;
import vn.edufit.verification.web.response.PendingVerificationQueueResponse;
import vn.edufit.verification.web.response.VerificationRequestDetailResponse;

@ExtendWith(MockitoExtension.class)
class AdminVerificationServiceTest {

  @Mock
  private VerificationRequestRepository verificationRequestRepository;

  @Mock
  private ProfileFacade profileFacade;

  private AdminVerificationService adminVerificationService;

  private CurrentUser adminUser;
  private UUID adminId;

  @BeforeEach
  void setUp() {
    adminVerificationService = new AdminVerificationService(
        verificationRequestRepository,
        profileFacade
    );

    adminId = UUID.randomUUID();
    adminUser = new CurrentUser() {
      @Override
      public UUID getUserId() {
        return adminId;
      }

      @Override
      public String getEmail() {
        return "admin@edufit.vn";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("ADMIN");
      }

      @Override
      public boolean hasRole(String role) {
        return "ADMIN".equalsIgnoreCase(role);
      }
    };
  }

  @Test
  @DisplayName("Lấy danh sách hàng đợi các gia sư đang chờ duyệt")
  void shouldGetPendingQueue() {
    VerificationRequest req = VerificationRequest.pending(UUID.randomUUID(), Instant.now());
    Page<VerificationRequest> page = new PageImpl<>(List.of(req));
    when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(
        eq(VerificationRequest.Status.PENDING),
        any(Pageable.class)
    )).thenReturn(page);

    PendingVerificationQueueResponse response = adminVerificationService.getPendingQueue(adminUser, 0, 10);
    assertNotNull(response);
    assertEquals(1, response.items().size());
    assertEquals("PENDING", response.items().getFirst().status());
  }

  @Test
  @DisplayName("Admin phê duyệt yêu cầu xác minh (APPROVE) -> Cập nhật trạng thái VERIFIED")
  void shouldApproveVerificationRequest() {
    UUID requestId = UUID.randomUUID();
    UUID tutorId = UUID.randomUUID();
    VerificationRequest req = VerificationRequest.pending(tutorId, Instant.now());

    when(verificationRequestRepository.findWithCredentialsByRequestId(requestId))
        .thenReturn(Optional.of(req));
    when(verificationRequestRepository.save(any(VerificationRequest.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ReviewVerificationRequest reviewRequest = new ReviewVerificationRequest(
        ReviewVerificationRequest.Action.APPROVE,
        null
    );

    VerificationRequestDetailResponse response = adminVerificationService.review(
        adminUser, requestId, reviewRequest
    );

    assertNotNull(response);
    assertEquals("APPROVED", response.status());
    assertEquals(adminId, response.reviewedBy());
    verify(profileFacade).markTutorVerified(eq(tutorId), any(Instant.class));
  }

  @Test
  @DisplayName("Admin từ chối yêu cầu xác minh (REJECT) -> Cập nhật trạng thái UNVERIFIED kèm lý do")
  void shouldRejectVerificationRequest() {
    UUID requestId = UUID.randomUUID();
    UUID tutorId = UUID.randomUUID();
    VerificationRequest req = VerificationRequest.pending(tutorId, Instant.now());

    when(verificationRequestRepository.findWithCredentialsByRequestId(requestId))
        .thenReturn(Optional.of(req));
    when(verificationRequestRepository.save(any(VerificationRequest.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ReviewVerificationRequest reviewRequest = new ReviewVerificationRequest(
        ReviewVerificationRequest.Action.REJECT,
        "Ảnh chụp văn bằng bị mờ, không rõ con dấu"
    );

    VerificationRequestDetailResponse response = adminVerificationService.review(
        adminUser, requestId, reviewRequest
    );

    assertNotNull(response);
    assertEquals("REJECTED", response.status());
    assertEquals("Ảnh chụp văn bằng bị mờ, không rõ con dấu", response.rejectionReason());
    assertEquals(adminId, response.reviewedBy());
    verify(profileFacade).markTutorRejected(tutorId);
  }

  @Test
  @DisplayName("Từ chối xử lý khi người dùng không có quyền ADMIN")
  void shouldRejectWhenNotAdmin() {
    CurrentUser nonAdmin = new CurrentUser() {
      @Override
      public UUID getUserId() {
        return UUID.randomUUID();
      }

      @Override
      public String getEmail() {
        return "student@edufit.vn";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("STUDENT");
      }

      @Override
      public boolean hasRole(String role) {
        return "STUDENT".equalsIgnoreCase(role);
      }
    };

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> adminVerificationService.getPendingQueue(nonAdmin, 0, 10)
    );
    assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
  }
}


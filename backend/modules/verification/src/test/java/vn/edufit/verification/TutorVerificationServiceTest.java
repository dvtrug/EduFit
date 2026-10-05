package vn.edufit.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;
import vn.edufit.storage.api.CredentialStorageService;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.verification.application.service.TutorVerificationService;
import vn.edufit.verification.infra.persistence.entity.CredentialType;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;
import vn.edufit.verification.infra.persistence.repository.VerificationRequestRepository;
import vn.edufit.verification.web.request.SubmitVerificationRequest;
import vn.edufit.verification.web.response.VerificationRequestDetailResponse;

@ExtendWith(MockitoExtension.class)
class TutorVerificationServiceTest {

  @Mock
  private VerificationRequestRepository verificationRequestRepository;

  @Mock
  private CredentialStorageService credentialStorageService;

  @Mock
  private ProfileFacade profileFacade;

  private TutorVerificationService tutorVerificationService;

  private CurrentUser tutorUser;
  private UUID userId;
  private UUID tutorId;

  @BeforeEach
  void setUp() {
    tutorVerificationService = new TutorVerificationService(
        verificationRequestRepository,
        credentialStorageService,
        profileFacade
    );

    userId = UUID.randomUUID();
    tutorId = UUID.randomUUID();
    tutorUser = new CurrentUser() {
      @Override
      public UUID getUserId() {
        return userId;
      }

      @Override
      public String getEmail() {
        return "tutor@test.com";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("TUTOR");
      }

      @Override
      public boolean hasRole(String role) {
        return "TUTOR".equalsIgnoreCase(role);
      }
    };
  }

  @Test
  @DisplayName("Nộp hồ sơ xác minh thành công và cập nhật trạng thái gia sư")
  void shouldSubmitVerificationSuccessfully() {
    when(profileFacade.getTutorIdByUserId(userId)).thenReturn(tutorId);
    when(verificationRequestRepository.existsByTutorIdAndStatus(tutorId, VerificationRequest.Status.PENDING))
        .thenReturn(false);

    MockMultipartFile file = new MockMultipartFile(
        "file", "degree.pdf", "application/pdf", "content".getBytes()
    );
    List<MultipartFile> files = List.of(file);

    StoredCredentialFile storedFile = new StoredCredentialFile(
        "degree.pdf", "key-123", "application/pdf", 7L
    );
    when(credentialStorageService.store(file)).thenReturn(storedFile);

    when(verificationRequestRepository.save(any(VerificationRequest.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubmitVerificationRequest request = new SubmitVerificationRequest(List.of(
        new SubmitVerificationRequest.CredentialItem(
            CredentialType.DEGREE,
            "Đại học Bách Khoa",
            (short) 2024,
            "Bằng cử nhân xuất sắc",
            List.of(0)
        )
    ));

    VerificationRequestDetailResponse response = tutorVerificationService.submit(
        tutorUser, request, files
    );

    assertNotNull(response);
    assertEquals("PENDING", response.status());
    assertEquals(1, response.credentials().size());
    verify(profileFacade).markTutorPendingVerification(tutorId);
  }

  @Test
  @DisplayName("Từ chối khi gia sư đã có hồ sơ PENDING (BR-15)")
  void shouldRejectWhenAlreadyHasPendingRequest() {
    when(profileFacade.getTutorIdByUserId(userId)).thenReturn(tutorId);
    when(verificationRequestRepository.existsByTutorIdAndStatus(tutorId, VerificationRequest.Status.PENDING))
        .thenReturn(true);

    SubmitVerificationRequest request = new SubmitVerificationRequest(List.of(
        new SubmitVerificationRequest.CredentialItem(
            CredentialType.DEGREE, "Đại học Bách Khoa", (short) 2024, "Ghi chú", List.of(0)
        )
    ));

    ResourceConflictException ex = assertThrows(
        ResourceConflictException.class,
        () -> tutorVerificationService.submit(tutorUser, request, List.of())
    );
    assertEquals(ErrorCode.CONFLICT_DETECTED, ex.getErrorCode());
  }

  @Test
  @DisplayName("Từ chối khi không có file tệp bằng cấp đính kèm")
  void shouldRejectWhenNoFilesProvided() {
    when(profileFacade.getTutorIdByUserId(userId)).thenReturn(tutorId);
    when(verificationRequestRepository.existsByTutorIdAndStatus(tutorId, VerificationRequest.Status.PENDING))
        .thenReturn(false);

    SubmitVerificationRequest request = new SubmitVerificationRequest(List.of(
        new SubmitVerificationRequest.CredentialItem(
            CredentialType.DEGREE, "Đại học Bách Khoa", (short) 2024, "Ghi chú", List.of(0)
        )
    ));

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> tutorVerificationService.submit(tutorUser, request, List.of())
    );
    assertEquals(ErrorCode.CREDENTIAL_NOT_SUBMITTED, ex.getErrorCode());
  }

  @Test
  @DisplayName("Rollback xóa file lưu trữ khi lưu CSDL xảy ra lỗi")
  void shouldRollbackStoredFilesWhenSaveFails() {
    when(profileFacade.getTutorIdByUserId(userId)).thenReturn(tutorId);
    when(verificationRequestRepository.existsByTutorIdAndStatus(tutorId, VerificationRequest.Status.PENDING))
        .thenReturn(false);

    MockMultipartFile file = new MockMultipartFile(
        "file", "cert.pdf", "application/pdf", "cert content".getBytes()
    );
    StoredCredentialFile storedFile = new StoredCredentialFile(
        "cert.pdf", "rollback-key-999", "application/pdf", 12L
    );
    when(credentialStorageService.store(file)).thenReturn(storedFile);
    when(verificationRequestRepository.save(any())).thenThrow(new RuntimeException("DB Connection down"));

    SubmitVerificationRequest request = new SubmitVerificationRequest(List.of(
        new SubmitVerificationRequest.CredentialItem(
            CredentialType.CERTIFICATE, "IELTS 8.0", (short) 2023, "IELTS IDP", List.of(0)
        )
    ));

    assertThrows(
        RuntimeException.class,
        () -> tutorVerificationService.submit(tutorUser, request, List.of(file))
    );

    verify(credentialStorageService).delete("rollback-key-999");
  }

  @Test
  @DisplayName("Lấy trạng thái yêu cầu xác minh gần nhất thành công")
  void shouldGetMyLatestRequestSuccessfully() {
    when(profileFacade.getTutorIdByUserId(userId)).thenReturn(tutorId);

    VerificationRequest req = VerificationRequest.pending(tutorId, Instant.now());
    when(verificationRequestRepository.findFirstByTutorIdOrderBySubmittedAtDesc(tutorId))
        .thenReturn(Optional.of(req));
    when(verificationRequestRepository.findWithCredentialsByRequestId(req.getRequestId()))
        .thenReturn(Optional.of(req));

    VerificationRequestDetailResponse response = tutorVerificationService.getMyLatestRequest(tutorUser);
    assertNotNull(response);
    assertEquals("PENDING", response.status());
  }

  @Test
  @DisplayName("Ném EntityNotFoundException khi gia sư chưa từng nộp hồ sơ xác minh")
  void shouldThrowWhenNoVerificationRequestFound() {
    when(profileFacade.getTutorIdByUserId(userId)).thenReturn(tutorId);
    when(verificationRequestRepository.findFirstByTutorIdOrderBySubmittedAtDesc(tutorId))
        .thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> tutorVerificationService.getMyLatestRequest(tutorUser)
    );
  }
}


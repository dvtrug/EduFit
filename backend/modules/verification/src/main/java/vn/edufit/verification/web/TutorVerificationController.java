package vn.edufit.verification.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.response.ApiResponse;
import vn.edufit.verification.application.service.TutorVerificationService;
import vn.edufit.verification.web.request.SubmitVerificationRequest;
import vn.edufit.verification.web.response.VerificationRequestDetailResponse;

/**
 * REST API Controller cho luồng nộp và tra cứu hồ sơ xác minh của Gia sư (ROLE_TUTOR).
 */
@RestController
@RequestMapping("/api/v1/verifications")
public class TutorVerificationController {

  private final TutorVerificationService tutorVerificationService;
  private final ObjectProvider<CurrentUser> currentUserProvider;

  public TutorVerificationController(
      TutorVerificationService tutorVerificationService,
      ObjectProvider<CurrentUser> currentUserProvider
  ) {
    this.tutorVerificationService = tutorVerificationService;
    this.currentUserProvider = currentUserProvider;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('TUTOR')")
  public ResponseEntity<ApiResponse<VerificationRequestDetailResponse>> submit(
      @Valid @RequestPart("payload") SubmitVerificationRequest request,
      @RequestPart("files") List<MultipartFile> files
  ) {
    VerificationRequestDetailResponse response = tutorVerificationService.submit(
        currentUser(),
        request,
        files
    );
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Nộp hồ sơ xác minh thành công"));
  }

  @GetMapping("/my-request")
  @PreAuthorize("hasRole('TUTOR')")
  public ResponseEntity<ApiResponse<VerificationRequestDetailResponse>> myRequest() {
    VerificationRequestDetailResponse response = tutorVerificationService.getMyLatestRequest(currentUser());
    return ResponseEntity.ok(ApiResponse.success(response, "Lấy trạng thái hồ sơ xác minh thành công"));
  }

  private CurrentUser currentUser() {
    CurrentUser currentUser = currentUserProvider.getIfAvailable();
    if (currentUser == null || currentUser.getUserId() == null) {
      throw new InvalidOperationException(
          ErrorCode.UNAUTHORIZED,
          "Phiên làm việc chưa được xác thực hoặc đã hết hạn"
      );
    }
    return currentUser;
  }
}


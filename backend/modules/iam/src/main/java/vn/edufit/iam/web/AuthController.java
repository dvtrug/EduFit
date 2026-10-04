package vn.edufit.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edufit.iam.application.AuthenticateAccountService;
import vn.edufit.iam.application.ChangePasswordService;
import vn.edufit.iam.application.RegisterAccountService;
import vn.edufit.iam.application.RequestPasswordResetService;
import vn.edufit.iam.application.ResetPasswordService;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.security.EduFitUserDetails;
import vn.edufit.iam.web.dto.AuthUserResponse;
import vn.edufit.iam.web.dto.ChangePasswordRequest;
import vn.edufit.iam.web.dto.ForgotPasswordRequest;
import vn.edufit.iam.web.dto.LoginRequest;
import vn.edufit.iam.web.dto.RegisterRequest;
import vn.edufit.iam.web.dto.ResetPasswordRequest;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.response.ApiResponse;

/**
 * REST API Controller phụ trách toàn bộ luồng Xác thực và Quản lý phiên (UC1.1 đến UC1.4).
 *
 * <p>Tuân thủ nghiêm ngặt hợp đồng tại {@code docs/authentication-contract.md}:
 * <ul>
 *   <li>Đăng ký kích hoạt tài khoản trực tiếp (Status: ACTIVE).</li>
 *   <li>Quản lý phiên stateful với cookie {@code EDUFIT_SESSION} (ADR-001 Decision 5).</li>
 *   <li>Cấu trúc phản hồi bọc chuẩn {@link ApiResponse}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final RegisterAccountService registerAccountService;
  private final AuthenticateAccountService authenticateAccountService;
  private final RequestPasswordResetService requestPasswordResetService;
  private final ResetPasswordService resetPasswordService;
  private final ChangePasswordService changePasswordService;
  private final AccountRepository accountRepository;
  private final ObjectProvider<CurrentUser> currentUserProvider;

  public AuthController(
      RegisterAccountService registerAccountService,
      AuthenticateAccountService authenticateAccountService,
      RequestPasswordResetService requestPasswordResetService,
      ResetPasswordService resetPasswordService,
      ChangePasswordService changePasswordService,
      AccountRepository accountRepository,
      ObjectProvider<CurrentUser> currentUserProvider
  ) {
    this.registerAccountService = registerAccountService;
    this.authenticateAccountService = authenticateAccountService;
    this.requestPasswordResetService = requestPasswordResetService;
    this.resetPasswordService = resetPasswordService;
    this.changePasswordService = changePasswordService;
    this.accountRepository = accountRepository;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Đăng ký tài khoản người dùng mới (UC1.1 - Kích hoạt ngay).
   */
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthUserResponse>> register(
      @Valid @RequestBody RegisterRequest request
  ) {
    Account account = registerAccountService.register(
        request.email(),
        request.password(),
        request.fullName(),
        request.role()
    );
    AuthUserResponse responseData = AuthUserResponse.from(account);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            responseData,
            "Đăng ký thành công. Bạn có thể đăng nhập ngay bây giờ."
        ));
  }

  /**
   * Đăng nhập người dùng và khởi tạo phiên làm việc (UC1.2).
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthUserResponse>> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest
  ) {
    Account account = authenticateAccountService.authenticate(request.email(), request.password());

    // Thiết lập phiên bảo mật Spring Security Session Context
    EduFitUserDetails userDetails = EduFitUserDetails.from(account);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    session.setAttribute("ACCOUNT_ID", account.getId());
    session.setAttribute("ROLE", account.getRole().name());

    return ResponseEntity.ok(ApiResponse.success(
        AuthUserResponse.from(account),
        "Đăng nhập thành công"
    ));
  }

  /**
   * Đăng xuất và hủy phiên làm việc hiện tại.
   */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công"));
  }

  /**
   * Yêu cầu đặt lại mật khẩu khi quên (UC1.3).
   */
  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request
  ) {
    requestPasswordResetService.requestReset(request.email());
    return ResponseEntity.ok(ApiResponse.ok(
        "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi."
    ));
  }

  /**
   * Đặt lại mật khẩu mới qua mã token (UC1.3).
   */
  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request
  ) {
    resetPasswordService.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.ok(ApiResponse.ok(
        "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."
    ));
  }

  /**
   * Đổi mật khẩu cho tài khoản đang đăng nhập (UC1.4).
   */
  @PostMapping("/change-password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request
  ) {
    CurrentUser currentUser = currentUserProvider.getIfAvailable();
    if (currentUser == null || currentUser.getUserId() == null) {
      throw new InvalidOperationException(
          ErrorCode.UNAUTHORIZED,
          "Yêu cầu cần được xác thực trước khi thực hiện đổi mật khẩu"
      );
    }

    changePasswordService.changePassword(
        currentUser.getUserId(),
        request.currentPassword(),
        request.newPassword()
    );
    return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công"));
  }

  /**
   * Lấy thông tin tài khoản người dùng hiện tại đang đăng nhập.
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<AuthUserResponse>> getCurrentUser() {
    CurrentUser currentUser = currentUserProvider.getIfAvailable();
    if (currentUser == null || currentUser.getUserId() == null) {
      throw new InvalidOperationException(
          ErrorCode.UNAUTHORIZED,
          "Phiên làm việc chưa được xác thực hoặc đã hết hạn"
      );
    }

    Account account = accountRepository.findById(currentUser.getUserId())
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

    return ResponseEntity.ok(ApiResponse.success(
        AuthUserResponse.from(account),
        "Lấy thông tin tài khoản thành công"
    ));
  }
}

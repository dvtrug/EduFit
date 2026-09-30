package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.application.PasswordPolicy;
import vn.edufit.iam.application.RegisterAccountService;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.security.Argon2PasswordService;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;

@DisplayName("Kiểm thử Đơn vị Đăng ký Tài khoản (RegisterAccountService - Mặc định ACTIVE)")
class RegisterAccountServiceTest {

  private AccountRepository accountRepository;
  private Argon2PasswordService passwordService;
  private PasswordPolicy passwordPolicy;
  private RegisterAccountService registerService;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepository.class);
    passwordService = mock(Argon2PasswordService.class);
    passwordPolicy = new PasswordPolicy();

    registerService = new RegisterAccountService(
        accountRepository,
        passwordService,
        passwordPolicy
    );

    when(passwordService.hash(any(String.class))).thenReturn("$argon2id$mocked_hash");
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("IAM-REG-001: Đăng ký thành công tạo tài khoản trực tiếp ACTIVE (BR-04 mới)")
  void shouldRegisterAccountSuccessfullyInActiveStatus() {
    when(accountRepository.existsByEmail("student@edufit.vn")).thenReturn(false);

    Account account = registerService.register(
        "Student@EduFit.vn ",
        "Password123@",
        " Nguyen Van A ",
        AccountRole.STUDENT
    );

    assertNotNull(account);
    assertEquals("student@edufit.vn", account.getEmail());
    assertEquals("Nguyen Van A", account.getFullName());
    assertEquals(AccountRole.STUDENT, account.getRole());
    assertEquals(AccountStatus.ACTIVE, account.getStatus(), "Tài khoản đăng ký mới phải ở trạng thái ACTIVE");
  }

  @Test
  @DisplayName("IAM-REG-003: Từ chối đăng ký khi email đã tồn tại trong hệ thống (BR-01)")
  void shouldRejectRegistrationWhenEmailAlreadyExists() {
    when(accountRepository.existsByEmail("tutor@edufit.vn")).thenReturn(true);

    ResourceConflictException ex = assertThrows(
        ResourceConflictException.class,
        () -> registerService.register(
            "tutor@edufit.vn",
            "Password123@",
            "Tutor B",
            AccountRole.TUTOR
        )
    );

    assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
  }

  @Test
  @DisplayName("IAM-REG-004: Chặn tuyệt đối tự đăng ký với vai trò ADMIN (BR-03)")
  void shouldRejectAdminRegistrationAttempt() {
    when(accountRepository.existsByEmail("admin@edufit.vn")).thenReturn(false);

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> registerService.register(
            "admin@edufit.vn",
            "Password123@",
            "Admin User",
            AccountRole.ADMIN
        )
    );

    assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
  }

  @Test
  @DisplayName("Từ chối khi mật khẩu không đạt chuẩn chính sách (BR-02)")
  void shouldRejectRegistrationWithWeakPassword() {
    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> registerService.register(
            "user@edufit.vn",
            "weak",
            "User C",
            AccountRole.STUDENT
        )
    );

    assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
  }
}

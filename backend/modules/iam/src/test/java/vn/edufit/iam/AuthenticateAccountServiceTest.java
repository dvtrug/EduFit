package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.application.AuthenticateAccountService;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.security.Argon2PasswordService;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

@DisplayName("Kiểm thử Đơn vị Đăng nhập & Khóa tài khoản (AuthenticateAccountService)")
class AuthenticateAccountServiceTest {

  private AccountRepository accountRepository;
  private Argon2PasswordService passwordService;
  private Clock fixedClock;
  private AuthenticateAccountService authService;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepository.class);
    passwordService = mock(Argon2PasswordService.class);

    fixedClock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);

    authService = new AuthenticateAccountService(
        accountRepository,
        passwordService,
        fixedClock
    );

    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("IAM-AUTH-001: Đăng nhập thành công khi mật khẩu đúng và tài khoản ACTIVE")
  void shouldAuthenticateSuccessfullyWhenCredentialsAreValid() {
    Account account = Account.of(
        UUID.randomUUID(),
        "student@edufit.vn",
        "$argon2id$hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT,
        AccountStatus.ACTIVE
    );

    when(accountRepository.findByEmail("student@edufit.vn")).thenReturn(Optional.of(account));
    when(passwordService.verify(eq(account.getPasswordHash()), eq("CorrectPassword123@"))).thenReturn(true);

    Account authenticated = authService.authenticate("student@edufit.vn", "CorrectPassword123@");

    assertNotNull(authenticated);
    assertEquals(0, authenticated.getFailedLoginAttempts());
    assertEquals(Instant.parse("2026-09-30T10:00:00Z"), authenticated.getLastLoginAt());
  }

  @Test
  @DisplayName("IAM-AUTH-002: Trả về cùng mã lỗi INVALID_CREDENTIALS khi sai mật khẩu hoặc sai email (NFR-06)")
  void shouldReturnSameErrorForWrongPasswordAndNonExistentEmail() {
    // Trường hợp 1: Email không tồn tại
    when(accountRepository.findByEmail("unknown@edufit.vn")).thenReturn(Optional.empty());
    InvalidOperationException ex1 = assertThrows(
        InvalidOperationException.class,
        () -> authService.authenticate("unknown@edufit.vn", "AnyPassword123@")
    );
    assertEquals(ErrorCode.INVALID_CREDENTIALS, ex1.getErrorCode());

    // Trường hợp 2: Sai mật khẩu
    Account account = Account.of(
        UUID.randomUUID(),
        "student@edufit.vn",
        "$argon2id$hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT,
        AccountStatus.ACTIVE
    );
    when(accountRepository.findByEmail("student@edufit.vn")).thenReturn(Optional.of(account));
    when(passwordService.verify(any(), eq("WrongPassword123@"))).thenReturn(false);

    InvalidOperationException ex2 = assertThrows(
        InvalidOperationException.class,
        () -> authService.authenticate("student@edufit.vn", "WrongPassword123@")
    );
    assertEquals(ErrorCode.INVALID_CREDENTIALS, ex2.getErrorCode());
  }

  @Test
  @DisplayName("IAM-AUTH-004: Khóa tài khoản 15 phút sau 5 lần nhập sai liên tiếp (BR-06)")
  void shouldLockAccountAfterFiveConsecutiveFailures() {
    Account account = Account.of(
        UUID.randomUUID(),
        "target@edufit.vn",
        "$argon2id$hashed...",
        "Target User",
        AccountRole.STUDENT,
        AccountStatus.ACTIVE
    );

    when(accountRepository.findByEmail("target@edufit.vn")).thenReturn(Optional.of(account));
    when(passwordService.verify(any(String.class), any(String.class))).thenReturn(false);

    // 4 lần đầu nhập sai: lỗi INVALID_CREDENTIALS
    for (int i = 1; i <= 4; i++) {
      InvalidOperationException ex = assertThrows(
          InvalidOperationException.class,
          () -> authService.authenticate("target@edufit.vn", "WrongPassword")
      );
      assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
      assertEquals(i, account.getFailedLoginAttempts());
    }

    // Lần thứ 5: Khóa tài khoản và ném ACCOUNT_LOCKED
    InvalidOperationException lockEx = assertThrows(
        InvalidOperationException.class,
        () -> authService.authenticate("target@edufit.vn", "WrongPassword")
    );
    assertEquals(ErrorCode.ACCOUNT_LOCKED, lockEx.getErrorCode());
    assertEquals(5, account.getFailedLoginAttempts());
    assertNotNull(account.getLockedUntil());
  }
}

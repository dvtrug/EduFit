package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import vn.edufit.iam.application.ChangePasswordService;
import vn.edufit.iam.application.PasswordPolicy;
import vn.edufit.iam.application.RequestPasswordResetService;
import vn.edufit.iam.application.ResetPasswordService;
import vn.edufit.iam.application.port.AccountEmailSender;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.persistence.AccountToken;
import vn.edufit.iam.infra.persistence.AccountTokenRepository;
import vn.edufit.iam.infra.persistence.TokenType;
import vn.edufit.iam.infra.security.Argon2PasswordService;
import vn.edufit.iam.infra.security.TokenCryptoService;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

@DisplayName("Kiểm thử Đơn vị Quản lý Mật khẩu (Reset & Change Password - UC1.3, UC1.4)")
class PasswordResetAndChangeTest {

  private AccountRepository accountRepository;
  private AccountTokenRepository accountTokenRepository;
  private Argon2PasswordService passwordService;
  private PasswordPolicy passwordPolicy;
  private TokenCryptoService tokenCryptoService;
  private AccountEmailSender emailSender;
  private Clock fixedClock;

  private RequestPasswordResetService requestResetService;
  private ResetPasswordService resetPasswordService;
  private ChangePasswordService changePasswordService;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepository.class);
    accountTokenRepository = mock(AccountTokenRepository.class);
    passwordService = mock(Argon2PasswordService.class);
    passwordPolicy = new PasswordPolicy();
    tokenCryptoService = new TokenCryptoService();
    emailSender = mock(AccountEmailSender.class);
    fixedClock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);

    requestResetService = new RequestPasswordResetService(
        accountRepository,
        accountTokenRepository,
        tokenCryptoService,
        emailSender,
        fixedClock
    );

    resetPasswordService = new ResetPasswordService(
        accountRepository,
        accountTokenRepository,
        passwordService,
        passwordPolicy,
        tokenCryptoService,
        fixedClock
    );

    changePasswordService = new ChangePasswordService(
        accountRepository,
        passwordService,
        passwordPolicy
    );

    when(passwordService.hash(any(String.class))).thenReturn("$argon2id$new_hash");
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("IAM-PWD-001: Yêu cầu reset mật khẩu trả về trung tính khi email không tồn tại (NFR-06)")
  void shouldReturnNeutralResultEvenWhenEmailDoesNotExist() {
    when(accountRepository.findByEmail("unknown@edufit.vn")).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> requestResetService.requestReset("unknown@edufit.vn"));
    verify(emailSender, never()).sendPasswordResetEmail(any(), any(), any());
  }

  @Test
  @DisplayName("IAM-PWD-002: Đặt lại mật khẩu thành công tiêu thụ token và cập nhật mật khẩu mới")
  void shouldResetPasswordSuccessfully() {
    Account account = Account.of(
        UUID.randomUUID(),
        "student@edufit.vn",
        "$argon2id$old_hash",
        "Student A",
        AccountRole.STUDENT,
        AccountStatus.ACTIVE
    );

    String rawToken = "reset-raw-token-1234567890";
    String tokenHash = tokenCryptoService.hashToken(rawToken);

    AccountToken token = AccountToken.create(
        account,
        TokenType.RESET_PASSWORD,
        tokenHash,
        Instant.parse("2026-09-30T10:30:00Z")
    );

    when(accountTokenRepository.findByTokenHashAndTokenType(eq(tokenHash), eq(TokenType.RESET_PASSWORD)))
        .thenReturn(Optional.of(token));
    when(passwordService.verify(eq(account.getPasswordHash()), eq("NewPassword123@"))).thenReturn(false);

    resetPasswordService.resetPassword(rawToken, "NewPassword123@");

    assertTrue(token.isUsed());
    assertEquals("$argon2id$new_hash", account.getPasswordHash());
  }

  @Test
  @DisplayName("IAM-PWD-003: Đổi mật khẩu từ chối khi mật khẩu hiện tại không đúng")
  void shouldRejectWhenCurrentPasswordIsIncorrect() {
    UUID accountId = UUID.randomUUID();
    Account account = Account.of(
        accountId,
        "user@edufit.vn",
        "$argon2id$correct_current_hash",
        "User D",
        AccountRole.STUDENT,
        AccountStatus.ACTIVE
    );

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(passwordService.verify(eq(account.getPasswordHash()), eq("WrongCurrentPass"))).thenReturn(false);

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> changePasswordService.changePassword(accountId, "WrongCurrentPass", "NewPassword123@")
    );

    assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, ex.getErrorCode());
  }

  @Test
  @DisplayName("IAM-PWD-004: Đổi mật khẩu từ chối khi mật khẩu mới trùng mật khẩu hiện tại")
  void shouldRejectWhenNewPasswordMatchesCurrentPassword() {
    UUID accountId = UUID.randomUUID();
    Account account = Account.of(
        accountId,
        "user@edufit.vn",
        "$argon2id$current_hash",
        "User D",
        AccountRole.STUDENT,
        AccountStatus.ACTIVE
    );

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(passwordService.verify(eq(account.getPasswordHash()), eq("SamePassword123@"))).thenReturn(true);

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> changePasswordService.changePassword(accountId, "SamePassword123@", "SamePassword123@")
    );

    assertEquals(ErrorCode.PASSWORD_REUSE_FORBIDDEN, ex.getErrorCode());
  }
}

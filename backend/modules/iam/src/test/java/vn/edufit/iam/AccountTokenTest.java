package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountToken;
import vn.edufit.iam.infra.persistence.TokenType;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

@DisplayName("Kiểm thử Đơn vị Thực thể AccountToken (One-Time Token Lifecycle)")
class AccountTokenTest {

  @Test
  @DisplayName("IAM-DOM-012: Khởi tạo Token hợp lệ với thời hạn chính xác")
  void shouldCreateValidTokenWithCorrectExpiry() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");
    Instant expiresAt = now.plus(TokenType.PASSWORD_RESET.getDefaultTtl());

    AccountToken token = AccountToken.create(
        account,
        TokenType.PASSWORD_RESET,
        "a3b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1",
        expiresAt
    );

    assertEquals(account, token.getAccount());
    assertEquals(TokenType.PASSWORD_RESET, token.getTokenType());
    assertNull(token.getUsedAt());
    assertFalse(token.isUsed());
    assertFalse(token.isExpired(now));
    assertTrue(token.isValid(now));
  }

  @Test
  @DisplayName("IAM-DOM-013: Tiêu thụ token thành công đúng một lần (BR-05)")
  void shouldConsumeTokenSuccessfullyOnce() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");
    AccountToken token = AccountToken.create(
        account,
        TokenType.PASSWORD_RESET,
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        now.plus(Duration.ofMinutes(30))
    );

    token.consume(now);

    assertTrue(token.isUsed());
    assertEquals(now, token.getUsedAt());
    assertFalse(token.isValid(now));
  }

  @Test
  @DisplayName("IAM-DOM-014: Từ chối tiêu thụ token đã hết hạn hoặc đã được sử dụng")
  void shouldRejectExpiredOrAlreadyUsedToken() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");
    Instant pastExpiry = now.minusSeconds(10);

    AccountToken expiredToken = AccountToken.create(
        account,
        TokenType.PASSWORD_RESET,
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        pastExpiry
    );

    assertThrows(InvalidOperationException.class, () -> expiredToken.consume(now));

    // Token đã dùng
    AccountToken usedToken = AccountToken.create(
        account,
        TokenType.PASSWORD_RESET,
        "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
        now.plus(Duration.ofMinutes(30))
    );
    usedToken.consume(now);

    assertThrows(InvalidOperationException.class, () -> usedToken.consume(now.plusSeconds(5)));
  }

  @Test
  @DisplayName("IAM-TOKEN-BOUNDARY-001: Token tại đúng thời điểm expiresAt được coi là đã hết hạn (BR-05)")
  void shouldConsiderTokenExpiredAtExactExpiryInstant() {
    Account account = Account.create(
      "studenta@gmail.com",
      "hashed...",
      "Nguyen Van A",
      AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");

    // Tạo token có thời điểm hết hạn ĐÚNG BẰNG thời điểm hiện tại 'now'
    AccountToken boundaryToken = AccountToken.create(
      account,
      TokenType.PASSWORD_RESET,
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      now
    );

    // Xác nhận tại đúng tích tắc now, token đã hết hạn và không hợp lệ
    assertTrue(boundaryToken.isExpired(now), "Tại đúng thời điểm expiresAt token phải hết hạn");
    assertFalse(boundaryToken.isValid(now), "Tại đúng thời điểm expiresAt token không được phép hợp lệ");

    // Cố tình tiêu thụ tại thời điểm này phải ném lỗi TOKEN_EXPIRED
    InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> boundaryToken.consume(now)
    );
    assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
  }

  @Test
  @DisplayName("IAM-TOKEN-HASH-001: Từ chối khởi tạo token nếu mã băm không đúng chuẩn SHA-256 hex 64 ký tự (NFR-03)")
  void shouldRejectTokenHashNotMeetingSha256HexRequirements() {
    Account account = Account.create(
      "student@edufit.vn",
      "hashed...",
      "Nguyen Van A",
      AccountRole.STUDENT
    );
    Instant expiresAt = Instant.parse("2026-09-29T11:00:00Z");

    // 1. Quá ngắn (chỉ 10 ký tự)
    assertThrows(InvalidOperationException.class, () ->
      AccountToken.create(account, TokenType.PASSWORD_RESET, "0123456789", expiresAt)
    );

    // 2. Đủ 64 ký tự nhưng chứa ký tự lạ 'z' (không phải hex [0-9a-fA-F])
    String invalidHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdez";
    InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> AccountToken.create(account, TokenType.PASSWORD_RESET, invalidHex, expiresAt)
    );
    assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
  }

}

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
import vn.edufit.shared.exception.InvalidOperationException;

@DisplayName("Kiểm thử Đơn vị Thực thể AccountToken (One-Time Token Lifecycle)")
class AccountTokenTest {

  @Test
  @DisplayName("IAM-DOM-012: Khởi tạo Token hợp lệ với thời hạn chính xác")
  void shouldCreateValidTokenWithCorrectExpiry() {
    Account account = Account.createPending(
        "student@edufit.vn",
        "hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");
    Instant expiresAt = now.plus(TokenType.EMAIL_VERIFICATION.getDefaultTtl());

    AccountToken token = AccountToken.create(
        account,
        TokenType.EMAIL_VERIFICATION,
        "a3b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1",
        expiresAt
    );

    assertEquals(account, token.getAccount());
    assertEquals(TokenType.EMAIL_VERIFICATION, token.getTokenType());
    assertNull(token.getUsedAt());
    assertFalse(token.isUsed());
    assertFalse(token.isExpired(now));
    assertTrue(token.isValid(now));
  }

  @Test
  @DisplayName("IAM-DOM-013: Tiêu thụ token thành công đúng một lần (BR-05)")
  void shouldConsumeTokenSuccessfullyOnce() {
    Account account = Account.createPending(
        "student@edufit.vn",
        "hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");
    AccountToken token = AccountToken.create(
        account,
        TokenType.EMAIL_VERIFICATION,
        "valid_hash_64_chars",
        now.plus(Duration.ofHours(24))
    );

    token.consume(now);

    assertTrue(token.isUsed());
    assertEquals(now, token.getUsedAt());
    assertFalse(token.isValid(now));
  }

  @Test
  @DisplayName("IAM-DOM-014: Từ chối tiêu thụ token đã hết hạn hoặc đã được sử dụng")
  void shouldRejectExpiredOrAlreadyUsedToken() {
    Account account = Account.createPending(
        "student@edufit.vn",
        "hashed...",
        "Nguyen Van A",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");
    Instant pastExpiry = now.minusSeconds(10);

    AccountToken expiredToken = AccountToken.create(
        account,
        TokenType.EMAIL_VERIFICATION,
        "expired_hash",
        pastExpiry
    );

    assertThrows(InvalidOperationException.class, () -> expiredToken.consume(now));

    // Token đã dùng
    AccountToken usedToken = AccountToken.create(
        account,
        TokenType.PASSWORD_RESET,
        "used_hash",
        now.plus(Duration.ofMinutes(30))
    );
    usedToken.consume(now);

    assertThrows(InvalidOperationException.class, () -> usedToken.consume(now.plusSeconds(5)));
  }
}

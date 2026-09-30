package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.shared.exception.InvalidOperationException;

@DisplayName("Kiểm thử Đơn vị Thực thể Account (Domain Logic & Lockout Rules)")
class AccountTest {

  @Test
  @DisplayName("IAM-DOM-001: Khởi tạo tài khoản mặc định ở trạng thái ACTIVE (BR-04)")
  void shouldInitializeAccountInActiveStatus() {
    Account account = Account.create(
        "Student@EduFit.vn ",
        "$argon2id$v=19$m=65536,t=3,p=1$hashed...",
        " Nguyen Van A ",
        AccountRole.STUDENT
    );

    assertEquals("student@edufit.vn", account.getEmail(), "Email phải được cắt khoảng trắng và viết thường");
    assertEquals("Nguyen Van A", account.getFullName(), "Họ tên phải được cắt khoảng trắng");
    assertEquals(AccountRole.STUDENT, account.getRole());
    assertEquals(AccountStatus.ACTIVE, account.getStatus());
    assertEquals(0, account.getFailedLoginAttempts());
    assertNull(account.getLockedUntil());
  }

  @Test
  @DisplayName("IAM-DOM-002: Chặn tuyệt đối tự đăng ký với vai trò ADMIN (BR-03)")
  void shouldPreventAdminRoleRegistration() {
    assertThrows(InvalidOperationException.class, () ->
        Account.create(
            "admin@edufit.vn",
            "hashed...",
            "Admin User",
            AccountRole.ADMIN
        )
    );
  }

  @Test
  @DisplayName("IAM-DOM-005: Đăng nhập sai 5 lần liên tiếp kích hoạt khóa tài khoản 15 phút (BR-06)")
  void shouldIncrementFailedAttemptsAndLockAccountAtFifthFailure() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Student C",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");

    for (int i = 1; i <= 4; i++) {
      account.recordLoginFailure(now);
      assertEquals(i, account.getFailedLoginAttempts());
      assertFalse(account.isLocked(now), "Chưa đủ 5 lần thì chưa bị khóa");
    }

    // Lần thứ 5 sai liên tiếp
    account.recordLoginFailure(now);
    assertEquals(5, account.getFailedLoginAttempts());
    assertTrue(account.isLocked(now), "Lần thứ 5 sai phải kích hoạt trạng thái khóa");
    assertEquals(now.plus(Duration.ofMinutes(15)), account.getLockedUntil());
  }

  @Test
  @DisplayName("IAM-DOM-006: canAuthenticate() trả về false khi tài khoản đang trong thời gian bị khóa")
  void shouldDenyLoginWhenAccountIsCurrentlyLocked() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Student D",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");

    for (int i = 0; i < 5; i++) {
      account.recordLoginFailure(now);
    }

    // Kiểm tra lúc 10:05 (đang trong 15 phút khóa)
    Instant fiveMinutesLater = now.plus(Duration.ofMinutes(5));
    assertTrue(account.isLocked(fiveMinutesLater));
    assertFalse(account.canAuthenticate(fiveMinutesLater));
  }

  @Test
  @DisplayName("IAM-DOM-007: Cho phép đăng nhập lại sau khi hết thời gian 15 phút khóa")
  void shouldAllowLoginAndResetCounterAfterLockExpires() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Student E",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");

    for (int i = 0; i < 5; i++) {
      account.recordLoginFailure(now);
    }

    // Thời điểm 16 phút sau
    Instant sixteenMinutesLater = now.plus(Duration.ofMinutes(16));
    assertFalse(account.isLocked(sixteenMinutesLater));
    assertTrue(account.canAuthenticate(sixteenMinutesLater));
  }

  @Test
  @DisplayName("IAM-DOM-008: Đăng nhập thành công xóa sạch bộ đếm lỗi và ghi nhận lastLoginAt (BR-06)")
  void shouldResetFailedAttemptsOnSuccessfulLogin() {
    Account account = Account.create(
        "student@edufit.vn",
        "hashed...",
        "Student F",
        AccountRole.STUDENT
    );
    Instant now = Instant.parse("2026-09-29T10:00:00Z");

    // Nhập sai 3 lần
    account.recordLoginFailure(now);
    account.recordLoginFailure(now);
    account.recordLoginFailure(now);
    assertEquals(3, account.getFailedLoginAttempts());

    // Đăng nhập đúng ở lần tiếp theo
    Instant loginTime = now.plusSeconds(30);
    account.recordSuccessfulLogin(loginTime);

    assertEquals(0, account.getFailedLoginAttempts());
    assertNull(account.getLockedUntil());
    assertEquals(loginTime, account.getLastLoginAt());
  }
}

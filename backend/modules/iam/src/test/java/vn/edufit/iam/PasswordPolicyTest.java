package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edufit.iam.application.PasswordPolicy;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

@DisplayName("Kiểm thử Đơn vị Chính sách Mật khẩu (PasswordPolicy - BR-02)")
class PasswordPolicyTest {

  private PasswordPolicy passwordPolicy;

  @BeforeEach
  void setUp() {
    passwordPolicy = new PasswordPolicy();
  }

  @Test
  @DisplayName("IAM-DOM-009: Chấp nhận mật khẩu hợp lệ (>= 8 ký tự, có cả chữ và số)")
  void shouldAcceptValidPassword() {
    assertDoesNotThrow(() -> passwordPolicy.validate("Password123@"));
    assertDoesNotThrow(() -> passwordPolicy.validate("Abc12345"));
    assertDoesNotThrow(() -> passwordPolicy.validate("StrongPass1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "short1", "1234567", "Abcdefg"})
  @DisplayName("IAM-DOM-010: Từ chối mật khẩu dưới 8 ký tự hoặc rỗng")
  void shouldRejectShortOrBlankPassword(String password) {
    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> passwordPolicy.validate(password)
    );
    assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
  }

  @Test
  @DisplayName("IAM-DOM-011: Từ chối mật khẩu không có số hoặc không có chữ cái")
  void shouldRejectPasswordWithoutDigitsOrLetters() {
    // Chỉ có chữ cái
    InvalidOperationException ex1 = assertThrows(
        InvalidOperationException.class,
        () -> passwordPolicy.validate("OnlyLettersLong")
    );
    assertEquals(ErrorCode.VALIDATION_FAILED, ex1.getErrorCode());

    // Chỉ có chữ số
    InvalidOperationException ex2 = assertThrows(
        InvalidOperationException.class,
        () -> passwordPolicy.validate("1234567890")
    );
    assertEquals(ErrorCode.VALIDATION_FAILED, ex2.getErrorCode());
  }
}

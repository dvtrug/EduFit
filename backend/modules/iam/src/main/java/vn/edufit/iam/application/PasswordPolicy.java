package vn.edufit.iam.application;

import org.springframework.stereotype.Component;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

/**
 * Chính sách kiểm tra độ mạnh của mật khẩu (BR-02).
 *
 * <p>Quy tắc theo SRS:
 * <ul>
 *   <li>Mật khẩu tối thiểu 8 ký tự.</li>
 *   <li>Phải chứa ít nhất 1 ký tự chữ cái (a-z, A-Z).</li>
 *   <li>Phải chứa ít nhất 1 chữ số (0-9).</li>
 * </ul>
 */
@Component
public class PasswordPolicy {

  private static final int MIN_LENGTH = 8;

  /**
   * Kiểm tra tính hợp lệ của mật khẩu.
   *
   * @param password mật khẩu thô
   * @throws InvalidOperationException nếu mật khẩu không thỏa mãn chính sách bảo mật
   */
  public void validate(String password) {
    if (password == null || password.isBlank()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Mật khẩu không được để trống"
      );
    }

    if (password.length() < MIN_LENGTH) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Mật khẩu phải có độ dài tối thiểu 8 ký tự"
      );
    }

    boolean hasLetter = false;
    boolean hasDigit = false;

    for (char c : password.toCharArray()) {
      if (Character.isLetter(c)) {
        hasLetter = true;
      } else if (Character.isDigit(c)) {
        hasDigit = true;
      }
      if (hasLetter && hasDigit) {
        break;
      }
    }

    if (!hasLetter || !hasDigit) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số"
      );
    }
  }
}

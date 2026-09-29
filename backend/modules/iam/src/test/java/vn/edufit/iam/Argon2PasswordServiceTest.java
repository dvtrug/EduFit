package vn.edufit.iam;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.iam.infra.security.Argon2PasswordService;

@DisplayName("Kiểm thử Đơn vị Dịch vụ Băm Mật khẩu Argon2id (NFR-01)")
class Argon2PasswordServiceTest {

  private Argon2PasswordService passwordService;

  @BeforeEach
  void setUp() {
    passwordService = new Argon2PasswordService();
  }

  @Test
  @DisplayName("Băm mật khẩu và so khớp thành công với mật khẩu đúng")
  void shouldHashAndVerifyPasswordSuccessfully() {
    String rawPassword = "StrongPassword123@";

    String hash = passwordService.hash(rawPassword);

    assertNotNull(hash);
    assertTrue(hash.startsWith("$argon2id$"), "Chuỗi hash phải theo chuẩn Argon2id");
    assertTrue(passwordService.verify(hash, rawPassword), "Mật khẩu đúng phải so khớp thành công");
  }

  @Test
  @DisplayName("Cùng một mật khẩu tạo ra các chuỗi hash khác nhau nhờ salt ngẫu nhiên")
  void shouldProduceDifferentHashesForSamePasswordDueToSalt() {
    String rawPassword = "SamePassword123#";

    String hash1 = passwordService.hash(rawPassword);
    String hash2 = passwordService.hash(rawPassword);

    assertNotEquals(hash1, hash2, "Mỗi lần băm phải sinh salt mới nên hash không được trùng nhau");
    assertTrue(passwordService.verify(hash1, rawPassword));
    assertTrue(passwordService.verify(hash2, rawPassword));
  }

  @Test
  @DisplayName("Từ chối khi so khớp với mật khẩu sai")
  void shouldRejectWrongPassword() {
    String correctPassword = "CorrectPassword123$";
    String wrongPassword = "WrongPassword456%";

    String hash = passwordService.hash(correctPassword);

    assertFalse(passwordService.verify(hash, wrongPassword), "Mật khẩu sai phải trả về false");
  }
}

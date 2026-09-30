package vn.edufit.iam.infra.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Service;

/**
 * Hiện thực cơ chế băm và xác thực mật khẩu sử dụng thuật toán Argon2id (NFR-01).
 *
 * <p>Tham số cấu hình bảo mật tiêu chuẩn OWASP:
 * <ul>
 *   <li>Thuật toán: Argon2id (chống tấn công GPU và Side-channel).</li>
 *   <li>Iterations: 3 vòng lặp.</li>
 *   <li>Memory: 65536 KiB (64 MiB RAM).</li>
 *   <li>Parallelism: 1 luồng xử lý.</li>
 * </ul>
 */
@Service
public class Argon2PasswordService {

  private static final int ITERATIONS = 3;
  private static final int MEMORY_KB = 65536;
  private static final int PARALLELISM = 1;

  private final Argon2 argon2;

  public Argon2PasswordService() {
    this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
  }

  /**
   * Băm mật khẩu dạng mảng ký tự thành chuỗi hash Argon2id an toàn.
   *
   * @param password mật khẩu thô
   * @return chuỗi hash Argon2id kèm salt ngẫu nhiên
   */
  public String hash(char[] password) {
    if (password == null || password.length == 0) {
      throw new IllegalArgumentException("Mật khẩu không được để trống");
    }
    try {
      return argon2.hash(ITERATIONS, MEMORY_KB, PARALLELISM, password);
    } finally {
      argon2.wipeArray(password);
    }
  }

  /**
   * Băm mật khẩu dạng String.
   */
  public String hash(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Mật khẩu không được để trống");
    }
    return hash(password.toCharArray());
  }

  /**
   * So khớp mật khẩu thô với chuỗi hash đã lưu trong CSDL.
   *
   * @param hash chuỗi hash trong CSDL
   * @param password mật khẩu người dùng nhập vào
   * @return true nếu mật khẩu trùng khớp
   */
  public boolean verify(String hash, char[] password) {
    if (hash == null || password == null) {
      return false;
    }
    try {
      return argon2.verify(hash, password);
    } finally {
      argon2.wipeArray(password);
    }
  }

  /**
   * So khớp mật khẩu String với chuỗi hash.
   */
  public boolean verify(String hash, String password) {
    if (hash == null || password == null) {
      return false;
    }
    return verify(hash, password.toCharArray());
  }
}

package vn.edufit.iam.infra.persistence;

import java.time.Duration;

/**
 * Phân loại mục đích của Token sử dụng một lần (One-Time Token).
 *
 * <p>Theo BR-05, token đặt lại mật khẩu có thời hạn 30 phút.
 */
public enum TokenType {
  /** Token xác thực địa chỉ email kích hoạt tài khoản hoặc thay đổi email (24 giờ). */
  VERIFY_EMAIL(Duration.ofHours(24)),

  /** Token đặt lại mật khẩu (BR-05, thời hạn 30 phút). */
  RESET_PASSWORD(Duration.ofMinutes(30));

  private final Duration defaultTtl;

  TokenType(Duration defaultTtl) {
    this.defaultTtl = defaultTtl;
  }

  public Duration getDefaultTtl() {
    return defaultTtl;
  }
}

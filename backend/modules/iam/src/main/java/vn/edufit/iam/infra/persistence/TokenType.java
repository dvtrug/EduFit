package vn.edufit.iam.infra.persistence;

import java.time.Duration;

/**
 * Phân loại mục đích của Token sử dụng một lần (One-Time Token).
 *
 * <p>Theo BR-05 (SRS):
 * <ul>
 *   <li>Token kích hoạt email có thời hạn 24 giờ.</li>
 *   <li>Token đặt lại mật khẩu có thời hạn 30 phút.</li>
 * </ul>
 */
public enum TokenType {
  EMAIL_VERIFICATION(Duration.ofHours(24)),
  PASSWORD_RESET(Duration.ofMinutes(30));

  private final Duration defaultTtl;

  TokenType(Duration defaultTtl) {
    this.defaultTtl = defaultTtl;
  }

  public Duration getDefaultTtl() {
    return defaultTtl;
  }
}

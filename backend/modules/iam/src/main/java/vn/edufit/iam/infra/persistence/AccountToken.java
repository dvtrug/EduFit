package vn.edufit.iam.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

/**
 * JPA Entity đại diện cho bảng {@code account_tokens} lưu trữ các Token dùng một lần (One-Time Token).
 *
 * <p>Quy tắc bảo mật và kiến trúc (BR-05, NFR-03, ADR-001, ADR-002):
 * <ul>
 *   <li><b>Khóa ngoại {@code user_id}:</b> Tham chiếu trực tiếp đến bảng {@code users(id)}.</li>
 *   <li><b>Tuyệt đối không lưu raw token:</b> Cột {@code token_hash} chỉ lưu chuỗi băm hex SHA-256 (64 ký tự).</li>
 *   <li><b>Dùng một lần (Single-use):</b> Phương thức {@link #consume(Instant)} đánh dấu {@code used_at} ngay lập tức để chống tấn công phát lại (Replay attack).</li>
 *   <li><b>Chuẩn hóa thời gian (NFR-18):</b> Lưu thời điểm hết hạn {@code expires_at} theo UTC qua kiểu dữ liệu {@link Instant}.</li>
 * </ul>
 */
@Entity
@Table(name = "email_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "token_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private Account account;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private TokenType tokenType;

  @Column(name = "token_hash", nullable = false, unique = true, length = 128)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  private static final java.util.regex.Pattern SHA256_HEX_PATTERN =
    java.util.regex.Pattern.compile("^[a-fA-F0-9]{64}$");
  /**
   * Tạo một Token mới gắn với tài khoản.
   *
   * @param account tài khoản sở hữu
   * @param tokenType loại token đặt lại mật khẩu
   * @param tokenHash mã băm SHA-256 (64 ký tự)
   * @param expiresAt thời điểm hết hạn
   */
  public static AccountToken create(
      Account account,
      TokenType tokenType,
      String tokenHash,
      Instant expiresAt
  ) {

    Objects.requireNonNull(account, "Tài khoản không được để trống");
    Objects.requireNonNull(tokenType, "Loại token không được để trống");
    Objects.requireNonNull(tokenHash, "Mã băm token không được để trống");
    Objects.requireNonNull(expiresAt, "Thời hạn hết hạn không được để trống");

    //Check xem token đã đúng 64 kí tự hay chưa
    if (!SHA256_HEX_PATTERN.matcher(tokenHash).matches()) {
      throw new InvalidOperationException(
        ErrorCode.INVALID_TOKEN,
        "Mã băm phải đủ 64 kí tự"
      );
    }

    AccountToken token = new AccountToken();
    token.account = account;
    token.tokenType = tokenType;
    token.tokenHash = tokenHash.toLowerCase();
    token.expiresAt = expiresAt;
    return token;
  }

  /**
   * Kiểm tra xem token đã quá hạn sử dụng hay chưa.
   */
  public boolean isExpired(Instant now) {
    return !now.isBefore(expiresAt);
  }

  /**
   * Kiểm tra xem token đã được tiêu thụ (sử dụng) hay chưa.
   */
  public boolean isUsed() {
    return usedAt != null;
  }

  /**
   * Kiểm tra xem token có hợp lệ để sử dụng tại thời điểm hiện tại hay không.
   */
  public boolean isValid(Instant now) {
    return !isExpired(now) && !isUsed();
  }

  /**
   * Tiêu thụ token một lần duy nhất (BR-05).
   *
   * @param now thời điểm tiêu thụ
   * @throws InvalidOperationException nếu token đã hết hạn hoặc đã được sử dụng trước đó
   */
  public void consume(Instant now) {
    if (isExpired(now)) {
      throw new InvalidOperationException(
          ErrorCode.TOKEN_EXPIRED,
          "Mã xác thực đã hết hạn sử dụng"
      );
    }
    if (isUsed()) {
      throw new InvalidOperationException(
          ErrorCode.TOKEN_ALREADY_USED,
          "Mã xác thực này đã được sử dụng trước đó"
      );
    }
    this.usedAt = now;
  }

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }
}

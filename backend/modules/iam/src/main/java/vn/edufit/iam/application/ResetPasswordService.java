package vn.edufit.iam.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.persistence.AccountToken;
import vn.edufit.iam.infra.persistence.AccountTokenRepository;
import vn.edufit.iam.infra.persistence.TokenType;
import vn.edufit.iam.infra.security.Argon2PasswordService;
import vn.edufit.iam.infra.security.TokenCryptoService;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

/**
 * Ca sử dụng Đặt lại mật khẩu mới bằng mã token reset (UC1.3).
 *
 * <p>Quy tắc:
 * <ul>
 *   <li><b>BR-02:</b> Mật khẩu mới phải vượt qua {@link PasswordPolicy}.</li>
 *   <li><b>BR-05:</b> Tiêu thụ token reset (chỉ dùng được 1 lần, hạn 30 phút).</li>
 *   <li><b>NFR-01:</b> Băm mật khẩu mới bằng Argon2id trước khi lưu.</li>
 * </ul>
 */
@Service
public class ResetPasswordService {

  private final AccountRepository accountRepository;
  private final AccountTokenRepository accountTokenRepository;
  private final Argon2PasswordService passwordService;
  private final PasswordPolicy passwordPolicy;
  private final TokenCryptoService tokenCryptoService;
  private final Clock clock;

  public ResetPasswordService(
      AccountRepository accountRepository,
      AccountTokenRepository accountTokenRepository,
      Argon2PasswordService passwordService,
      PasswordPolicy passwordPolicy,
      TokenCryptoService tokenCryptoService,
      Clock clock
  ) {
    this.accountRepository = accountRepository;
    this.accountTokenRepository = accountTokenRepository;
    this.passwordService = passwordService;
    this.passwordPolicy = passwordPolicy;
    this.tokenCryptoService = tokenCryptoService;
    this.clock = clock;
  }

  /**
   * Đặt lại mật khẩu mới thông qua token xác thực.
   *
   * @param rawToken    mã token thô
   * @param newPassword mật khẩu mới
   */
  @Transactional
  public void resetPassword(String rawToken, String newPassword) {
    Objects.requireNonNull(rawToken, "Mã token không được để trống");
    Objects.requireNonNull(newPassword, "Mật khẩu mới không được để trống");

    // 1. Kiểm tra chính sách mật khẩu (BR-02)
    passwordPolicy.validate(newPassword);

    Instant now = clock.instant();
    String tokenHash = tokenCryptoService.hashToken(rawToken);

    // 2. Tìm kiếm và kiểm tra tính hợp lệ của token
    AccountToken token = accountTokenRepository
        .findByTokenHashAndTokenType(tokenHash, TokenType.RESET_PASSWORD)
        .orElseThrow(() -> new InvalidOperationException(
            ErrorCode.INVALID_TOKEN,
            "Mã token đặt lại mật khẩu không hợp lệ hoặc không tồn tại"
        ));

    if (token.isUsed()) {
      throw new InvalidOperationException(
          ErrorCode.TOKEN_ALREADY_USED,
          "Mã token đặt lại mật khẩu đã được sử dụng trước đó"
      );
    }

    if (token.isExpired(now)) {
      throw new InvalidOperationException(
          ErrorCode.TOKEN_EXPIRED,
          "Mã token đặt lại mật khẩu đã hết hạn sử dụng (quá 30 phút)"
      );
    }

    Account account = token.getAccount();

    // 3. Kiểm tra mật khẩu mới không được trùng với mật khẩu hiện tại
    if (passwordService.verify(account.getPasswordHash(), newPassword)) {
      throw new InvalidOperationException(
          ErrorCode.PASSWORD_REUSE_FORBIDDEN,
          "Mật khẩu mới không được trùng với mật khẩu gần nhất"
      );
    }

    // 4. Tiêu thụ token
    token.consume(now);

    // 5. Cập nhật mật khẩu mới băm bằng Argon2id
    String newHash = passwordService.hash(newPassword);
    account.changePassword(newHash);
    accountRepository.save(account);
  }
}

package vn.edufit.iam.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.iam.application.port.AccountEmailSender;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.persistence.AccountToken;
import vn.edufit.iam.infra.persistence.AccountTokenRepository;
import vn.edufit.iam.infra.persistence.TokenType;
import vn.edufit.iam.infra.security.TokenCryptoService;

/**
 * Ca sử dụng Yêu cầu đặt lại mật khẩu (UC1.3 - Quên mật khẩu).
 *
 * <p>Quy tắc bảo mật:
 * <ul>
 *   <li><b>NFR-06 (Enumeration Resistance):</b> Phản hồi trung tính dù email có tồn tại trong hệ thống hay không.</li>
 *   <li><b>BR-05:</b> Sinh mã token đặt lại mật khẩu (hạn sử dụng 30 phút).</li>
 * </ul>
 */
@Service
public class RequestPasswordResetService {

  private final AccountRepository accountRepository;
  private final AccountTokenRepository accountTokenRepository;
  private final TokenCryptoService tokenCryptoService;
  private final AccountEmailSender emailSender;
  private final Clock clock;

  public RequestPasswordResetService(
      AccountRepository accountRepository,
      AccountTokenRepository accountTokenRepository,
      TokenCryptoService tokenCryptoService,
      AccountEmailSender emailSender,
      Clock clock
  ) {
    this.accountRepository = accountRepository;
    this.accountTokenRepository = accountTokenRepository;
    this.tokenCryptoService = tokenCryptoService;
    this.emailSender = emailSender;
    this.clock = clock;
  }

  /**
   * Xử lý yêu cầu quên mật khẩu.
   *
   * @param email địa chỉ email của người dùng
   */
  @Transactional
  public void requestReset(String email) {
    Objects.requireNonNull(email, "Email không được để trống");

    String normalizedEmail = Account.normalizeEmail(email);
    Account account = accountRepository.findByEmail(normalizedEmail).orElse(null);

    // Không tồn tại thì không làm gì để tránh rò rỉ thông tin người dùng (NFR-06)
    if (account == null) {
      return;
    }

    Instant now = clock.instant();

    // Vô hiệu hóa các token reset trước đó của tài khoản này
    accountTokenRepository.invalidatePreviousTokens(account, TokenType.PASSWORD_RESET, now);

    // Sinh token mới 256-bit (hạn 30 phút)
    String rawToken = tokenCryptoService.generateRawToken();
    String tokenHash = tokenCryptoService.hashToken(rawToken);
    Instant expiresAt = now.plus(TokenType.PASSWORD_RESET.getDefaultTtl());

    AccountToken token = AccountToken.create(
        account,
        TokenType.PASSWORD_RESET,
        tokenHash,
        expiresAt
    );
    accountTokenRepository.save(token);

    emailSender.sendPasswordResetEmail(account.getEmail(), rawToken, account.getFullName());
  }
}

package vn.edufit.iam.application;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.security.Argon2PasswordService;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

/**
 * Ca sử dụng Đổi mật khẩu cho người dùng đang đăng nhập (UC1.4).
 *
 * <p>Quy tắc:
 * <ul>
 *   <li><b>BR-02:</b> Mật khẩu mới phải đáp ứng {@link PasswordPolicy}.</li>
 *   <li><b>Xác thực mật khẩu cũ:</b> Người dùng phải nhập đúng mật khẩu hiện tại.</li>
 *   <li><b>Chống trùng mật khẩu:</b> Mật khẩu mới không được trùng mật khẩu hiện tại.</li>
 * </ul>
 */
@Service
public class ChangePasswordService {

  private final AccountRepository accountRepository;
  private final Argon2PasswordService passwordService;
  private final PasswordPolicy passwordPolicy;

  public ChangePasswordService(
      AccountRepository accountRepository,
      Argon2PasswordService passwordService,
      PasswordPolicy passwordPolicy
  ) {
    this.accountRepository = accountRepository;
    this.passwordService = passwordService;
    this.passwordPolicy = passwordPolicy;
  }

  /**
   * Thay đổi mật khẩu người dùng.
   *
   * @param accountId       mã định danh tài khoản người dùng
   * @param currentPassword mật khẩu hiện tại
   * @param newPassword     mật khẩu mới
   */
  @Transactional
  public void changePassword(UUID accountId, String currentPassword, String newPassword) {
    Objects.requireNonNull(accountId, "ID tài khoản không được để trống");
    Objects.requireNonNull(currentPassword, "Mật khẩu hiện tại không được để trống");
    Objects.requireNonNull(newPassword, "Mật khẩu mới không được để trống");

    // 1. Kiểm tra chính sách mật khẩu mới (BR-02)
    passwordPolicy.validate(newPassword);

    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản người dùng"));

    // 2. Xác minh mật khẩu hiện tại
    if (!passwordService.verify(account.getPasswordHash(), currentPassword)) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_CURRENT_PASSWORD,
          "Mật khẩu hiện tại không chính xác"
      );
    }

    // 3. Mật khẩu mới không được trùng với mật khẩu hiện tại
    if (currentPassword.equals(newPassword)) {
      throw new InvalidOperationException(
          ErrorCode.PASSWORD_REUSE_FORBIDDEN,
          "Mật khẩu mới không được trùng với mật khẩu hiện tại"
      );
    }

    // 4. Băm và lưu mật khẩu mới
    String newHash = passwordService.hash(newPassword);
    account.changePassword(newHash);
    accountRepository.save(account);
  }
}

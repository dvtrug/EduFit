package vn.edufit.iam.application;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;
import vn.edufit.iam.infra.security.Argon2PasswordService;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.ResourceConflictException;

/**
 * Ca sử dụng Đăng ký tài khoản người dùng mới (UC1.1).
 *
 * <p>Quy tắc nghiệp vụ:
 * <ul>
 *   <li><b>BR-01:</b> Email được chuẩn hóa (trim, lowercase) và không trùng lặp.</li>
 *   <li><b>BR-02:</b> Mật khẩu phải vượt qua {@link PasswordPolicy}.</li>
 *   <li><b>BR-03:</b> Chỉ cho phép vai trò STUDENT, PARENT, TUTOR. Cấm ADMIN.</li>
 *   <li><b>BR-04 (Mới):</b> Tài khoản đăng ký mới kích hoạt ngay ở trạng thái {@code ACTIVE}, không cần xác thực email.</li>
 *   <li><b>NFR-01:</b> Mật khẩu được băm bằng thuật toán Argon2id.</li>
 * </ul>
 */
@Service
public class RegisterAccountService {

  private final AccountRepository accountRepository;
  private final Argon2PasswordService passwordService;
  private final PasswordPolicy passwordPolicy;

  public RegisterAccountService(
      AccountRepository accountRepository,
      Argon2PasswordService passwordService,
      PasswordPolicy passwordPolicy
  ) {
    this.accountRepository = accountRepository;
    this.passwordService = passwordService;
    this.passwordPolicy = passwordPolicy;
  }

  /**
   * Đăng ký tài khoản người dùng mới (mặc định ACTIVE).
   *
   * @param email    địa chỉ email
   * @param password mật khẩu thô
   * @param fullName họ và tên
   * @param role     vai trò đăng ký
   * @return {@link Account} đã tạo thành công
   */
  @Transactional
  public Account register(String email, String password, String fullName, AccountRole role) {
    Objects.requireNonNull(email, "Email không được để trống");
    Objects.requireNonNull(password, "Mật khẩu không được để trống");
    Objects.requireNonNull(fullName, "Họ tên không được để trống");
    Objects.requireNonNull(role, "Vai trò không được để trống");

    // 1. Kiểm tra chính sách mật khẩu (BR-02)
    passwordPolicy.validate(password);

    // 2. Chuẩn hóa email và kiểm tra trùng lặp (BR-01)
    String normalizedEmail = Account.normalizeEmail(email);
    if (accountRepository.existsByEmail(normalizedEmail)) {
      throw new ResourceConflictException(
          ErrorCode.USER_ALREADY_EXISTS,
          "Email đã được đăng ký bởi một tài khoản khác trong hệ thống"
      );
    }

    // 3. Băm mật khẩu bằng Argon2id (NFR-01)
    String passwordHash = passwordService.hash(password);

    // 4. Tạo tài khoản trực tiếp ở trạng thái ACTIVE (BR-03, BR-04)
    Account account = Account.create(normalizedEmail, passwordHash, fullName, role);
    return accountRepository.save(account);
  }
}

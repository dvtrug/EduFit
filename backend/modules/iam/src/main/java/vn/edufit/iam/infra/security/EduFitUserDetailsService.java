package vn.edufit.iam.infra.security;

import java.util.Objects;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.iam.infra.persistence.Account;
import vn.edufit.iam.infra.persistence.AccountRepository;

/**
 * Triển khai chuẩn của Spring Security {@link UserDetailsService} trong module IAM.
 *
 * <p>Mục đích thiết kế:
 * <ul>
 *   <li>Tải thông tin tài khoản người dùng từ bảng {@code accounts} qua {@link AccountRepository}.</li>
 *   <li>Chuyển đổi thành đối tượng {@link EduFitUserDetails} tương thích với Spring Security Context.</li>
 *   <li>Đặt tại module IAM để các cấu hình bảo mật có thể tái sử dụng mà không phụ thuộc ngược lên module {@code app}.</li>
 * </ul>
 */
@Service
public class EduFitUserDetailsService implements UserDetailsService {

  private final AccountRepository accountRepository;

  public EduFitUserDetailsService(AccountRepository accountRepository) {
    this.accountRepository = Objects.requireNonNull(accountRepository, "AccountRepository không được phép null");
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (username == null || username.isBlank()) {
      throw new UsernameNotFoundException("Tên đăng nhập / Email không được để trống");
    }

    String normalizedEmail = Account.normalizeEmail(username);
    Account account = accountRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new UsernameNotFoundException(
            "Không tìm thấy tài khoản người dùng với email: " + username
        ));

    return EduFitUserDetails.from(account);
  }
}

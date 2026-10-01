package vn.edufit.iam.infra.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.infra.persistence.Account;

/**
 * Lớp đại diện cho Principal người dùng trong Spring Security Context của EduFit.
 *
 * <p>Mục đích thiết kế và lý do kiến trúc:
 * <ul>
 *   <li><b>Thuộc module IAM:</b> Được quản lý tập trung tại {@code modules/iam}, tránh vi phạm kiến trúc
 *       (module nghiệp vụ không phụ thuộc ngược lên module {@code app}).</li>
 *   <li><b>Triển khai {@link UserDetails}:</b> Tương thích hoàn toàn với kiến trúc bảo mật của Spring Security.</li>
 *   <li><b>Mang theo {@code userId} (UUID):</b> UserDetails mặc định của Spring chỉ có username (chuỗi ký tự).
 *       Lớp này bổ sung thuộc tính {@code userId} kiểu UUID để các Service nghiệp vụ có thể lấy ID người dùng
 *       ngay trong bộ nhớ phiên làm việc mà không cần query lại database.</li>
 *   <li><b>Phân quyền Role chuẩn (NFR-07):</b> Hỗ trợ các vai trò chính: STUDENT, PARENT, TUTOR, ADMIN.
 *       Tự động chuẩn hóa tiền tố {@code ROLE_} cho các {@link GrantedAuthority}.</li>
 * </ul>
 */
public class EduFitUserDetails implements UserDetails {

  private final UUID userId;
  private final String email;
  private final String password;
  private final Set<GrantedAuthority> authorities;
  private final boolean enabled;
  private final boolean accountNonLocked;

  /**
   * Khởi tạo đầy đủ thông tin chi tiết của người dùng trong Security Context.
   *
   * @param userId Mã định danh duy nhất của người dùng.
   * @param email Địa chỉ email đăng nhập (đồng thời là username).
   * @param password Mật khẩu đã được băm (hoặc chuỗi rỗng nếu đã xác thực xong).
   * @param authorities Danh sách quyền/vai trò được cấp.
   * @param enabled Trạng thái kích hoạt của tài khoản.
   * @param accountNonLocked Trạng thái không bị khóa tạm thời.
   */
  public EduFitUserDetails(
      UUID userId,
      String email,
      String password,
      Collection<? extends GrantedAuthority> authorities,
      boolean enabled,
      boolean accountNonLocked
  ) {
    this.userId = Objects.requireNonNull(userId, "userId không được phép null");
    this.email = Objects.requireNonNull(email, "email không được phép null");
    this.password = password != null ? password : "";
    this.authorities = authorities != null ? Set.copyOf(authorities) : Collections.emptySet();
    this.enabled = enabled;
    this.accountNonLocked = accountNonLocked;
  }

  /**
   * Factory method tiện ích khởi tạo nhanh đối tượng người dùng từ thông tin cơ bản và tập vai trò.
   *
   * @param userId Mã định danh UUID.
   * @param email Email người dùng.
   * @param roles Tập các tên vai trò (ví dụ: "STUDENT", "TUTOR").
   * @return Đối tượng {@link EduFitUserDetails}.
   */
  public static EduFitUserDetails of(UUID userId, String email, Set<String> roles) {
    Set<GrantedAuthority> grantedAuthorities = roles == null ? Collections.emptySet() : roles.stream()
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toUnmodifiableSet());
    return new EduFitUserDetails(userId, email, "", grantedAuthorities, true, true);
  }

  /**
   * Factory method tạo {@link EduFitUserDetails} từ JPA Entity {@link Account}.
   *
   * @param account Thực thể tài khoản người dùng từ cơ sở dữ liệu.
   * @return Đối tượng {@link EduFitUserDetails} tương ứng.
   */
  public static EduFitUserDetails from(Account account) {
    Objects.requireNonNull(account, "Account không được để trống");
    String roleName = account.getRole() != null ? account.getRole().name() : "";
    String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
    Set<GrantedAuthority> authorities = authority.isBlank()
        ? Collections.emptySet()
        : Set.of(new SimpleGrantedAuthority(authority));

    boolean enabled = account.getStatus() == AccountStatus.ACTIVE;
    boolean accountNonLocked = !account.isLocked(Instant.now());

    return new EduFitUserDetails(
        account.getId(),
        account.getEmail(),
        account.getPasswordHash(),
        authorities,
        enabled,
        accountNonLocked
    );
  }

  /**
   * Lấy mã UUID định danh của người dùng.
   *
   * @return UUID người dùng.
   */
  public UUID getUserId() {
    return userId;
  }

  /**
   * Lấy địa chỉ email của người dùng.
   *
   * @return Email người dùng.
   */
  public String getEmail() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}

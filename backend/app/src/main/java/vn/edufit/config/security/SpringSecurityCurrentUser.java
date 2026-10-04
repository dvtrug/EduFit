package vn.edufit.config.security;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.edufit.iam.infra.security.EduFitUserDetails;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ForbiddenOperationException;

/**
 * Lớp triển khai thực tế của interface {@link CurrentUser}, trích xuất danh tính từ Spring Security Context.
 *
 * <p>Mục đích thiết kế và nguyên lý Dependency Inversion (DIP):
 * <ul>
 *   <li><b>Tách biệt nghiệp vụ và hạ tầng:</b> Tầng nghiệp vụ (Domain & Application) ở các module con
 *       ({@code scheduling}, {@code profile}...) chỉ phụ thuộc vào interface trừu tượng {@link CurrentUser}
 *       ở {@code platform/shared}, hoàn toàn không hề biết Spring Security là gì.</li>
 *   <li><b>Dễ dàng Unit Test:</b> Khi viết unit test cho các Service, lập trình viên có thể truyền vào một mock
 *       {@code CurrentUser} giả lập đơn giản mà không cần khởi tạo toàn bộ bộ khung Spring Security phức tạp.</li>
 *   <li><b>Truy xuất trong bộ nhớ:</b> Khi ứng dụng chạy thật, class này lấy thông tin từ {@link SecurityContextHolder},
 *       trả về {@code userId}, {@code email} và {@code roles} tức thì mà không phải query lại database.</li>
 * </ul>
 */
@Component
public class SpringSecurityCurrentUser implements CurrentUser {

  @Override
  public UUID getUserId() {
    EduFitUserDetails userDetails = getAuthenticatedUserDetails();
    return userDetails.getUserId();
  }

  @Override
  public String getEmail() {
    EduFitUserDetails userDetails = getAuthenticatedUserDetails();
    return userDetails.getEmail();
  }

  @Override
  public Set<String> getRoles() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return Collections.emptySet();
    }

    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean hasRole(String role) {
    if (role == null || role.isBlank()) {
      return false;
    }
    String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;
    return getRoles().contains(normalizedRole);
  }

  /**
   * Phương thức nội bộ trích xuất thông tin người dùng từ SecurityContextHolder.
   *
   * @return Đối tượng {@link EduFitUserDetails} của phiên đăng nhập hiện tại.
   * @throws ForbiddenOperationException Nếu người dùng chưa đăng nhập hoặc phiên làm việc đã hết hạn.
   */
  private EduFitUserDetails getAuthenticatedUserDetails() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      throw new ForbiddenOperationException("Yêu cầu cần được xác thực hoặc phiên đăng nhập đã hết hạn.");
    }

    Object principal = auth.getPrincipal();
    if (principal instanceof EduFitUserDetails userDetails) {
      return userDetails;
    }

    if (principal instanceof String principalStr) {
      try {
        UUID parsedUserId = UUID.fromString(principalStr);
        return EduFitUserDetails.of(parsedUserId, auth.getName(), Collections.emptySet());
      } catch (IllegalArgumentException ignored) {
        // Not a UUID string
      }
    }

    throw new ForbiddenOperationException("Thông tin danh tính người dùng trong phiên làm việc không hợp lệ.");
  }
}

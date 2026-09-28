package vn.edufit.shared.auth;

import java.util.Set;
import java.util.UUID;

/**
 * Interface đại diện cho danh tính và quyền hạn của người dùng đang thực hiện yêu cầu.
 *
 * <p>Áp dụng nguyên lý Đảo ngược phụ thuộc (Dependency Inversion Principle - DIP):
 * <ul>
 *   <li>Tầng nghiệp vụ (Domain & Application) ở mọi module chỉ phụ thuộc vào Interface này để
 *       truy vấn ID và Role của người dùng hiện tại mà không cần quan tâm thông tin đó được lấy từ
 *       Session Cookie, JWT hay Header.</li>
 *   <li>Lớp triển khai thật (CurrentUserImpl) sẽ nằm ở module {@code app}, nơi có Spring Security Context.</li>
 *   <li>Khi viết Unit Test, lập trình viên có thể dễ dàng tạo đối tượng giả (Mock/Stub) mà không cần khởi động Spring Security.</li>
 * </ul>
 *
 * <p>Đáp ứng yêu cầu NFR-07 (Role-Based Access Control) trong tài liệu SRS EduFit.
 */
public interface CurrentUser {

  /**
   * Lấy mã định danh duy nhất (UUID) của người dùng hiện tại trong hệ thống.
   *
   * @return UUID của người dùng.
   */
  UUID getUserId();

  /**
   * Lấy địa chỉ email đăng nhập của người dùng hiện tại.
   *
   * @return Email dưới dạng chuỗi ký tự.
   */
  String getEmail();

  /**
   * Lấy danh sách các vai trò (roles) được gán cho người dùng hiện tại (ví dụ: STUDENT, PARENT, TUTOR, ADMIN).
   *
   * @return Tập hợp (Set) các tên vai trò.
   */
  Set<String> getRoles();

  /**
   * Kiểm tra nhanh xem người dùng hiện tại có sở hữu một vai trò cụ thể hay không.
   *
   * @param role Tên vai trò cần kiểm tra (ví dụ: "TUTOR").
   * @return {@code true} nếu người dùng có vai trò này, ngược lại {@code false}.
   */
  boolean hasRole(String role);
}

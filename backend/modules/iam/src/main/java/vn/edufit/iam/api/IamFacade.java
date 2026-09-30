package vn.edufit.iam.api;

import java.util.Optional;
import java.util.UUID;
import vn.edufit.iam.api.dto.UserSummaryView;

/**
 * Bề mặt công khai (Facade) của module IAM dành cho các module khác trong hệ thống.
 *
 * <p>Theo quy tắc kiến trúc ADR-002: Các module bên ngoài (ví dụ {@code connection}, {@code scheduling})
 * CHỈ được phép tiêm phụ thuộc (Inject) và gọi vào {@link IamFacade}, tuyệt đối không truy cập trực tiếp
 * vào tầng nội bộ (Entity, Repository, Service) của IAM.
 */
public interface IamFacade {

  /**
   * Tìm kiếm thông tin tóm tắt an toàn của người dùng theo ID.
   *
   * @param id mã định danh tài khoản
   * @return {@link Optional} chứa {@link UserSummaryView} nếu tìm thấy
   */
  Optional<UserSummaryView> findUserSummaryById(UUID id);

  /**
   * Kiểm tra sự tồn tại của người dùng trong hệ thống theo ID.
   *
   * @param id mã định danh tài khoản
   * @return true nếu tài khoản tồn tại
   */
  boolean existsById(UUID id);
}

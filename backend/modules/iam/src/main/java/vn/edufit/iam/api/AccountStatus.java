package vn.edufit.iam.api;

/**
 * Trạng thái của tài khoản trong hệ thống EduFit.
 *
 * <p>Quy tắc trạng thái:
 * <ul>
 *   <li>Mới đăng ký ➔ {@code ACTIVE} (BR-04).</li>
 *   <li>Bị khóa/ngừng hoạt động ➔ {@code DEACTIVATED}.</li>
 * </ul>
 */
public enum AccountStatus {
  /** Tài khoản đang hoạt động bình thường. */
  ACTIVE,

  /** Tài khoản đã bị vô hiệu hóa bởi Quản trị viên. */
  DEACTIVATED
}

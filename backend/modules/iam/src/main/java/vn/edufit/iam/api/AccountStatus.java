package vn.edufit.iam.api;

/**
 * Trạng thái của tài khoản trong hệ thống EduFit.
 *
 * <p>Quy tắc chuyển trạng thái:
 * <ul>
 *   <li>Mới đăng ký ➔ {@code PENDING_VERIFICATION} (BR-04).</li>
 *   <li>Kích hoạt email thành công ➔ {@code ACTIVE}.</li>
 *   <li>Bị khóa/ngừng hoạt động ➔ {@code DEACTIVATED}.</li>
 * </ul>
 */
public enum AccountStatus {
  /** Tài khoản vừa tạo, đang chờ người dùng bấm link kích hoạt gửi qua email. */
  PENDING_VERIFICATION,

  /** Tài khoản đã kích hoạt email và đang hoạt động bình thường. */
  ACTIVE,

  /** Tài khoản đã bị vô hiệu hóa bởi Quản trị viên. */
  DEACTIVATED;

  /**
   * Kiểm tra xem việc chuyển từ trạng thái hiện tại sang trạng thái mới có hợp lệ hay không.
   *
   * @param nextStatus trạng thái đích muốn chuyển sang
   * @return true nếu chuyển trạng thái hợp lệ
   */
  public boolean canTransitionTo(AccountStatus nextStatus) {
    if (nextStatus == null || this == nextStatus) {
      return false;
    }
    return switch (this) {
      case PENDING_VERIFICATION -> nextStatus == ACTIVE || nextStatus == DEACTIVATED;
      case ACTIVE -> nextStatus == DEACTIVATED;
      case DEACTIVATED -> false; // Trạng thái cuối, không thể phục hồi tự ý
    };
  }
}

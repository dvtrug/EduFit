package vn.edufit.iam.api;

/**
 * Định nghĩa các vai trò (Role) của người dùng trong hệ thống EduFit.
 *
 * <p>Theo BR-03 (SRS): Người dùng đăng ký công khai chỉ được phép chọn vai trò STUDENT, PARENT hoặc TUTOR.
 * Vai trò ADMIN được cấp phát nội bộ và tuyệt đối không cho phép tự đăng ký qua giao diện.
 */
public enum AccountRole {
  STUDENT,
  PARENT,
  TUTOR,
  ADMIN;

  /**
   * Kiểm tra xem vai trò này có được phép tự do đăng ký qua form công khai hay không.
   *
   * @return true nếu là STUDENT, PARENT hoặc TUTOR; false nếu là ADMIN
   */
  public boolean isSelfRegistrable() {
    return this != ADMIN;
  }
}

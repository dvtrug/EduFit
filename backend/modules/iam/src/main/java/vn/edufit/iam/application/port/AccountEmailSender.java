package vn.edufit.iam.application.port;

/**
 * Cổng giao tiếp gửi thư điện tử của phân hệ IAM (Ports & Adapters).
 *
 * <p>Mục đích:
 * <ul>
 *   <li>Tách biệt nghiệp vụ cốt lõi khỏi hạ tầng mạng và máy chủ SMTP thật.</li>
 *   <li>Cho phép dễ dàng thay thế bằng Mock/Fake trong các bài kiểm thử tự động.</li>
 * </ul>
 */
public interface AccountEmailSender {

  /**
   * Gửi thư chứa liên kết đặt lại mật khẩu kèm token 30 phút.
   *
   * @param recipientEmail email người nhận
   * @param rawToken       chuỗi mã token đặt lại mật khẩu thô
   * @param fullName       họ và tên người nhận
   */
  void sendPasswordResetEmail(String recipientEmail, String rawToken, String fullName);
}

package vn.edufit.shared.exception;

import java.util.Objects;

/**
 * Lớp cơ sở trừu tượng (Abstract Base Class) cho mọi ngoại lệ nghiệp vụ trong hệ thống EduFit.
 *
 * <p>Mục đích thiết kế:
 * <ul>
 *   <li><b>Kế thừa {@link RuntimeException}:</b> Là ngoại lệ Unchecked, giúp code sạch hơn (không cần khai báo
 *       {@code throws} rườm rà ở mỗi hàm). Quan trọng nhất: Trong Spring Framework, mọi {@link RuntimeException}
 *       mặc định sẽ kích hoạt cơ chế <b>Rollback giao dịch cơ sở dữ liệu</b> (đáp ứng NFR-11 về tính toàn vẹn Atomic Transactions).</li>
 *   <li><b>Độc lập với Spring & HTTP:</b> Không sử dụng annotation {@code @ResponseStatus} ở đây.
 *       Việc bắt ngoại lệ và ánh xạ sang HTTP Status Code (400, 404, 409...) sẽ được thực hiện tập trung
 *       tại {@code GlobalExceptionHandler} của module {@code app}.</li>
 *   <li><b>Luôn gắn liền với {@link ErrorCode}:</b> Giúp định danh chính xác nguyên nhân lỗi.</li>
 * </ul>
 */
public abstract class BaseBusinessException extends RuntimeException {

  /** Mã lỗi định danh duy nhất cho loại ngoại lệ này. */
  private final ErrorCode errorCode;

  /**
   * Khởi tạo ngoại lệ nghiệp vụ với mã lỗi và thông điệp giải thích.
   *
   * @param errorCode Mã định danh lỗi (không được null).
   * @param message   Thông điệp mô tả chi tiết lỗi phát sinh.
   */
  protected BaseBusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = Objects.requireNonNull(errorCode, "Mã lỗi (errorCode) không được phép null");
  }

  /**
   * Khởi tạo ngoại lệ nghiệp vụ với mã lỗi, thông điệp và nguyên nhân gốc (cause).
   *
   * @param errorCode Mã định danh lỗi.
   * @param message   Thông điệp mô tả chi tiết lỗi.
   * @param cause     Ngoại lệ gốc gây ra lỗi này.
   */
  protected BaseBusinessException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = Objects.requireNonNull(errorCode, "Mã lỗi (errorCode) không được phép null");
  }

  /**
   * Lấy mã lỗi định danh của ngoại lệ.
   *
   * @return Đối tượng {@link ErrorCode}.
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}

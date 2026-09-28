package vn.edufit.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("Kiểm thử hệ thống Ngoại lệ nghiệp vụ")
class BusinessExceptionTest {

  @Test
  @DisplayName("EntityNotFoundException khởi tạo đúng mã RESOURCE_NOT_FOUND")
  void shouldInitializeEntityNotFoundExceptionCorrectly() {
    EntityNotFoundException ex = EntityNotFoundException.of("Session", "sess-123");

    assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    assertEquals("Không tìm thấy Session với mã định danh: sess-123", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("ResourceConflictException giữ đúng mã lỗi định danh")
  void shouldInitializeResourceConflictExceptionCorrectly() {
    ResourceConflictException ex = new ResourceConflictException(
        ErrorCode.SCHEDULE_OVERLAP, "Buổi học bị trùng lịch"
    );

    assertEquals(ErrorCode.SCHEDULE_OVERLAP, ex.getErrorCode());
    assertEquals("Buổi học bị trùng lịch", ex.getMessage());
  }

  @Test
  @DisplayName("ForbiddenOperationException mang mã lỗi FORBIDDEN")
  void shouldInitializeForbiddenOperationExceptionCorrectly() {
    ForbiddenOperationException ex = new ForbiddenOperationException("Bạn không có quyền duyệt hồ sơ này");

    assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    assertEquals("Bạn không có quyền duyệt hồ sơ này", ex.getMessage());
  }

  @Test
  @DisplayName("InvalidOperationException mang mã lỗi INVALID_OPERATION")
  void shouldInitializeInvalidOperationExceptionCorrectly() {
    InvalidOperationException ex = new InvalidOperationException("Buổi học đã hủy không thể chấp nhận");

    assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
    assertEquals("Buổi học đã hủy không thể chấp nhận", ex.getMessage());
  }
}

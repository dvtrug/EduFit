package vn.edufit.web.advice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử đơn vị cho {@link GlobalExceptionHandler} sử dụng MockMvc chế độ standalone.
 *
 * <p>Mục đích:
 * <ul>
 *   <li>Xác minh từng ngoại lệ nghiệp vụ (Business Exception) bắn ra từ tầng ứng dụng
 *       đều được bắt chính xác và chuyển thành mã trạng thái HTTP chuẩn mực (400, 403, 404, 409, 500).</li>
 *   <li>Xác minh cấu trúc JSON phản hồi tuân thủ hoàn hảo đặc tả {@link vn.edufit.shared.response.ApiError}.</li>
 *   <li>Chạy ở mức Standalone Unit Test siêu tốc (chỉ vài mili-giây), không cần nạp context nặng nề.</li>
 * </ul>
 */
@DisplayName("Kiểm thử GlobalExceptionHandler (Ánh xạ Ngoại lệ sang HTTP)")
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new StubController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Nested
  @DisplayName("1. Ánh xạ các Ngoại lệ nghiệp vụ thuần Java")
  class BusinessExceptionMappingTests {

    @Test
    @DisplayName("EntityNotFoundException phải map thành HTTP 404 Not Found")
    void shouldReturn404WhenEntityNotFound() throws Exception {
      mockMvc.perform(get("/test/not-found"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code", is(ErrorCode.RESOURCE_NOT_FOUND.name())))
          .andExpect(jsonPath("$.message", notNullValue()))
          .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("ResourceConflictException phải map thành HTTP 409 Conflict")
    void shouldReturn409WhenResourceConflict() throws Exception {
      mockMvc.perform(get("/test/conflict"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is(ErrorCode.SCHEDULE_OVERLAP.name())))
          .andExpect(jsonPath("$.message", notNullValue()))
          .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("ForbiddenOperationException phải map thành HTTP 403 Forbidden")
    void shouldReturn403WhenForbidden() throws Exception {
      mockMvc.perform(get("/test/forbidden"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code", is(ErrorCode.FORBIDDEN.name())))
          .andExpect(jsonPath("$.message", notNullValue()))
          .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("InvalidOperationException phải map thành HTTP 400 Bad Request")
    void shouldReturn400WhenInvalidOperation() throws Exception {
      mockMvc.perform(get("/test/invalid-operation"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is(ErrorCode.INVALID_OPERATION.name())))
          .andExpect(jsonPath("$.message", notNullValue()))
          .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
  }

  @Nested
  @DisplayName("2. Ánh xạ lỗi Kiểm thực dữ liệu (Validation) & Lỗi máy chủ (500)")
  class ValidationAndSystemErrorTests {

    @Test
    @DisplayName("Body thiếu trường bắt buộc phải map thành HTTP 400 kèm chi tiết từng trường")
    void shouldReturn400WithFieldDetailsWhenValidationFails() throws Exception {
      String invalidJson = "{\"title\": \"\"}";

      mockMvc.perform(post("/test/validate")
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_FAILED.name())))
          .andExpect(jsonPath("$.details[0].field", is("title")))
          .andExpect(jsonPath("$.details[0].message", notNullValue()))
          .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Lỗi hệ thống bất ngờ (Exception) phải map thành HTTP 500 an toàn")
    void shouldReturn500SafeResponseWhenUnexpectedCrashOccurs() throws Exception {
      mockMvc.perform(get("/test/crash"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code", is(ErrorCode.INTERNAL_SERVER_ERROR.name())))
          .andExpect(jsonPath("$.message", notNullValue()))
          .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
  }

  // =========================================================================
  // CONTROLLER GIẢ LẬP (STUB) DÙNG CHO KIỂM THỬ
  // =========================================================================

  @RestController
  static class StubController {

    @GetMapping("/test/not-found")
    public void throwNotFound() {
      throw EntityNotFoundException.of("người dùng", "12345");
    }

    @GetMapping("/test/conflict")
    public void throwConflict() {
      throw new ResourceConflictException(ErrorCode.SCHEDULE_OVERLAP, "Khung giờ bị trùng lịch học");
    }

    @GetMapping("/test/forbidden")
    public void throwForbidden() {
      throw new ForbiddenOperationException("Bạn không có quyền thực hiện thao tác này");
    }

    @GetMapping("/test/invalid-operation")
    public void throwInvalidOperation() {
      throw new InvalidOperationException("Thao tác không hợp lệ với trạng thái hiện tại");
    }

    @PostMapping("/test/validate")
    public void validateInput(@Valid @RequestBody SampleRequest request) {
      // Endpoint kiểm tra validation
    }

    @GetMapping("/test/crash")
    public void throwCrash() {
      throw new RuntimeException("Cơ sở dữ liệu bị ngắt kết nối đột ngột!");
    }
  }

  record SampleRequest(
      @NotBlank(message = "Tiêu đề không được để trống")
      String title
  ) {}
}

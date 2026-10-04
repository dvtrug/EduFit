package vn.edufit.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.application.OutputSanitizer;

@DisplayName("Kiểm thử đơn vị cho OutputSanitizer")
class OutputSanitizerTest {

  private final OutputSanitizer sanitizer = new OutputSanitizer();

  @Test
  @DisplayName("Khi chuỗi đầu vào hợp lệ, trả về chuỗi đã cắt tỉa khoảng trắng")
  void shouldTrimValidText() {
    String raw = "   Đây là phản hồi từ AI   ";
    String sanitized = sanitizer.sanitize(raw);

    assertThat(sanitized).isEqualTo("Đây là phản hồi từ AI");
  }

  @Test
  @DisplayName("Khi chuỗi đầu vào là null, ném ngoại lệ AiUnavailableException")
  void shouldThrowWhenInputIsNull() {
    assertThatThrownBy(() -> sanitizer.sanitize(null))
        .isInstanceOf(AiUnavailableException.class)
        .hasMessageContaining("rỗng");
  }

  @Test
  @DisplayName("Khi chuỗi đầu vào chỉ chứa khoảng trắng, ném ngoại lệ AiUnavailableException")
  void shouldThrowWhenInputIsBlank() {
    assertThatThrownBy(() -> sanitizer.sanitize("   \n\t  "))
        .isInstanceOf(AiUnavailableException.class)
        .hasMessageContaining("rỗng");
  }

  @Test
  @DisplayName("Khi chuỗi đầu vào vượt quá 4000 ký tự, tự động cắt tỉa về 4000 ký tự")
  void shouldTruncateWhenExceedingMaxLength() {
    String longText = "A".repeat(5000);
    String sanitized = sanitizer.sanitize(longText);

    assertThat(sanitized).hasSize(4000);
  }
}

package vn.edufit.profile.web.request;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Yêu cầu sinh gợi ý tiểu sử gia sư (Bio) thông qua AI.
 *
 * @param displayName     Tên hiển thị của gia sư (tùy chọn, tự động lấy từ profile nếu để trống).
 * @param subjects        Danh sách tên môn học giảng dạy (tùy chọn).
 * @param educationLevels Danh sách tên cấp học / khối lớp (tùy chọn).
 * @param experienceYears Số năm kinh nghiệm giảng dạy (tùy chọn).
 * @param tone            Phong cách văn phong: "friendly", "professional", "concise".
 * @param focusArea       Định hướng trọng tâm: "exam", "foundation", "mindset".
 * @param customPrompt    Gợi ý / thế mạnh bổ sung của gia sư (tối đa 500 ký tự).
 * @param currentBio      Nội dung bản thảo / ý tưởng gia sư tự viết sẵn (tùy chọn, tối đa 2000 ký tự).
 */
public record GenerateTutorBioRequest(
    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    String displayName,

    List<String> subjects,

    List<String> educationLevels,

    Integer experienceYears,

    String tone,

    String focusArea,

    @Size(max = 500, message = "Yêu cầu bổ sung không được vượt quá 500 ký tự")
    String customPrompt,

    @Size(max = 2000, message = "Nội dung bản nháp không được vượt quá 2000 ký tự")
    String currentBio
) {
  public GenerateTutorBioRequest(
      String displayName,
      List<String> subjects,
      List<String> educationLevels,
      Integer experienceYears,
      String tone,
      String focusArea,
      String customPrompt
  ) {
    this(displayName, subjects, educationLevels, experienceYears, tone, focusArea, customPrompt, null);
  }

  public String resolvedTone() {
    return (tone != null && !tone.isBlank()) ? tone.trim().toLowerCase() : "friendly";
  }

  public String resolvedFocusArea() {
    return (focusArea != null && !focusArea.isBlank()) ? focusArea.trim().toLowerCase() : "exam";
  }
}

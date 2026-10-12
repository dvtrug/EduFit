package vn.edufit.profile.web.response;

/**
 * Kết quả sinh gợi ý tiểu sử gia sư từ AI Gateway hoặc Fallback Template.
 *
 * @param bio            Nội dung tiểu sử được sinh ra.
 * @param isAiGenerated  true nếu sinh từ LLM thật, false nếu dùng bản mẫu dự phòng.
 * @param remainingQuota Số lượt yêu cầu còn lại trong phiên (BR-10).
 */
public record GenerateTutorBioResponse(
    String bio,
    boolean isAiGenerated,
    int remainingQuota
) {}

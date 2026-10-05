package vn.edufit.profile.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;

public record UpdateTutorProfileRequest(
    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 100, message = "Tên hiển thị không được vượt quá 100 ký tự")
    String displayName,

    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    String headline,

    String bio,

    @NotNull(message = "Hình thức giảng dạy không được để trống")
    TeachingMode teachingMode,

    @Size(max = 100, message = "Khu vực không được vượt quá 100 ký tự")
    String area,

    @NotNull(message = "Học phí mỗi buổi không được để trống")
    @Min(value = 0, message = "Học phí không thể âm")
    Long pricePerSession,

    @Min(value = 0, message = "Số năm kinh nghiệm không thể âm")
    Short experienceYears,

    String teachingMethod
) {}

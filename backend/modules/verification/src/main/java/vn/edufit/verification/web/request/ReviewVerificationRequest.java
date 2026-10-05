package vn.edufit.verification.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewVerificationRequest(
    @NotNull(message = "Hành động duyệt hồ sơ là bắt buộc")
    Action action,

    @Size(max = 1000, message = "Lý do từ chối tối đa 1000 ký tự")
    String reason
) {

  public enum Action {
    APPROVE,
    REJECT
  }
}

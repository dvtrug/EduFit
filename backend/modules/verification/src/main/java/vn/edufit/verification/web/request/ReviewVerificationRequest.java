package vn.edufit.verification.web.request;

import jakarta.validation.constraints.NotNull;

public record ReviewVerificationRequest(
    @NotNull(message = "Hành động duyệt không được để trống")
    Action action,
    String reason
) {

  public enum Action {
    APPROVE,
    REJECT
  }
}


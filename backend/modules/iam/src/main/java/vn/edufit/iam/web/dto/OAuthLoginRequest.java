package vn.edufit.iam.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload yêu cầu xác thực bằng OAuth2 bên thứ 3 (Google, Zalo,...).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthLoginRequest(
    String idToken,
    String provider,
    String email,
    String fullName,
    String providerUserId,
    String avatarUrl
) {
  public String getEffectiveProvider() {
    return (provider != null && !provider.isBlank()) ? provider.toUpperCase() : "GOOGLE";
  }
}

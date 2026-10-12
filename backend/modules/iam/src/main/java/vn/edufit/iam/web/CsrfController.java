package vn.edufit.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edufit.shared.response.ApiResponse;

@RestController
public class CsrfController {
  @GetMapping("/api/v1/auth/csrf")
  public ApiResponse<TokenResponse> csrf(HttpServletRequest request, HttpServletResponse response) {
    // Return the plain cookie token, matching the SPA header resolver (not the XOR attribute).
    CsrfToken token =
        ((DeferredCsrfToken) request.getAttribute(DeferredCsrfToken.class.getName())).get();
    response.setHeader("Cache-Control", "no-store");
    return ApiResponse.success(
        new TokenResponse(token.getToken(), token.getHeaderName(), token.getParameterName()),
        "CSRF token generated successfully");
  }

  public record TokenResponse(String token, String headerName, String parameterName) {}
}

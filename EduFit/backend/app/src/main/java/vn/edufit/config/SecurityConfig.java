package vn.edufit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.response.ApiError;

/**
 * Cấu hình bảo mật hệ thống toàn diện sử dụng Spring Security.
 *
 * <p>Mục đích thiết kế và tuân thủ quyết định kiến trúc:
 * <ul>
 *   <li><b>ADR-001 Decision 5 (Primary Track):</b> Cơ chế Server-side Session với Opaque Token lưu qua Cookie {@code httpOnly}.
 *       Thỏa mãn hoàn hảo <b>NFR-04</b> (hết hạn sau 60 phút không thao tác, có thể hủy phiên tức thời) và
 *       <b>FR-04</b> (hủy toàn bộ phiên đang hoạt động khác khi người dùng đổi mật khẩu).</li>
 *   <li><b>NFR-01 (Mật khẩu an toàn):</b> Bắt buộc sử dụng thuật toán băm <b>Argon2</b> thông qua {@link Argon2PasswordEncoder},
 *       chống lại các cuộc tấn công Brute-force và Rainbow Table bằng cách cấu hình chi phí bộ nhớ và thời gian băm.</li>
 *   <li><b>NFR-07 (Role-Based Access Control):</b> Kích hoạt {@link EnableMethodSecurity} để các module nghiệp vụ
 *       có thể bảo vệ từng hàm nghiệp vụ bằng {@code @PreAuthorize("hasRole('TUTOR')")}.</li>
 *   <li><b>RESTful Error Response:</b> Ghi đè {@link AuthenticationEntryPoint} và {@link AccessDeniedHandler} để khi gặp
 *       lỗi 401 hoặc 403, hệ thống trả về đúng định dạng JSON {@link ApiError} thay vì trang HTML mặc định của Spring.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Cấu hình chuỗi lọc bảo mật (Security Filter Chain) cho các yêu cầu HTTP.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. Cấu hình bảo vệ chống tấn công CSRF (dùng cookie XSRF-TOKEN cho Next.js gửi qua header X-XSRF-TOKEN)
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers(
                "/api/v1/auth/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/**"
            )
        )

        // 2. Cấu hình phân quyền truy cập URL
        .authorizeHttpRequests(auth -> auth
            // Các đường dẫn công khai (không cần đăng nhập)
            .requestMatchers(
                "/api/v1/auth/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/health",
                "/error"
            ).permitAll()
            // Mọi yêu cầu API nghiệp vụ khác đều bắt buộc phải đăng nhập
            .anyRequest().authenticated()
        )

        // 3. Quản lý phiên làm việc (Session Management) theo ADR-001 Decision 5
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .maximumSessions(5)
            .maxSessionsPreventsLogin(false)
        )

        // 4. Xử lý phản hồi lỗi xác thực và phân quyền chuẩn REST JSON
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(restAuthenticationEntryPoint())
            .accessDeniedHandler(restAccessDeniedHandler())
        );

    return http.build();
  }

  /**
   * Bean mã hóa mật khẩu chuẩn Argon2 đáp ứng yêu cầu NFR-01.
   * <p>Sử dụng các thông số chuẩn của Spring Security: saltLength=16, hashLength=32, parallelism=1, memory=16384, iterations=2.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  /**
   * Bộ xử lý trả về HTTP 401 kèm định dạng ApiError JSON khi người dùng chưa xác thực.
   */
  @Bean
  public AuthenticationEntryPoint restAuthenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      ApiError apiError = ApiError.of(
          ErrorCode.UNAUTHORIZED.name(),
          "Yêu cầu cần được xác thực hoặc phiên đăng nhập đã hết hạn."
      );
      response.getWriter().write(objectMapper.writeValueAsString(apiError));
    };
  }

  /**
   * Bộ xử lý trả về HTTP 403 kèm định dạng ApiError JSON khi người dùng không đủ quyền.
   */
  @Bean
  public AccessDeniedHandler restAccessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      ApiError apiError = ApiError.of(
          ErrorCode.FORBIDDEN.name(),
          "Bạn không có quyền thực hiện hành động này trong hệ thống."
      );
      response.getWriter().write(objectMapper.writeValueAsString(apiError));
    };
  }
}

package vn.edufit.config.security;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edufit.iam.infra.security.EduFitUserDetails;
import vn.edufit.shared.exception.ForbiddenOperationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đơn vị cho {@link SpringSecurityCurrentUser} chứng minh nguyên lý Dependency Inversion.
 */
@DisplayName("Kiểm thử SpringSecurityCurrentUser (Trích xuất danh tính từ SecurityContext)")
class SpringSecurityCurrentUserTest {

  private SpringSecurityCurrentUser currentUser;

  @BeforeEach
  void setUp() {
    currentUser = new SpringSecurityCurrentUser();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Khi đã đăng nhập: Trích xuất chính xác userId, email và tập roles")
  void shouldExtractUserInfoWhenAuthenticated() {
    UUID expectedUserId = UUID.randomUUID();
    String expectedEmail = "tutor@edufit.vn";
    Set<String> roles = Set.of("TUTOR");

    EduFitUserDetails userDetails = EduFitUserDetails.of(expectedUserId, expectedEmail, roles);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertEquals(expectedUserId, currentUser.getUserId());
    assertEquals(expectedEmail, currentUser.getEmail());
    assertTrue(currentUser.hasRole("TUTOR"));
    assertTrue(currentUser.hasRole("ROLE_TUTOR")); // Kiểm tra hỗ trợ cả tiền tố ROLE_
    assertFalse(currentUser.hasRole("STUDENT"));
    assertNotNull(currentUser.getRoles());
    assertTrue(currentUser.getRoles().contains("TUTOR"));
  }

  @Test
  @DisplayName("Khi chưa đăng nhập: Gọi getUserId() phải ném ForbiddenOperationException")
  void shouldThrowForbiddenWhenUnauthenticated() {
    SecurityContextHolder.clearContext();

    assertThrows(ForbiddenOperationException.class, () -> currentUser.getUserId());
    assertThrows(ForbiddenOperationException.class, () -> currentUser.getEmail());
    assertTrue(currentUser.getRoles().isEmpty());
    assertFalse(currentUser.hasRole("TUTOR"));
  }
}

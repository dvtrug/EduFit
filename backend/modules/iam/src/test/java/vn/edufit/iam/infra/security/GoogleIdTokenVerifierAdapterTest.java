package vn.edufit.iam.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.iam.application.OAuthUserInfo;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

class GoogleIdTokenVerifierAdapterTest {

  private GoogleIdTokenVerifier mockGoogleVerifier;
  private GoogleIdTokenVerifierAdapter adapter;

  @BeforeEach
  void setUp() {
    mockGoogleVerifier = mock(GoogleIdTokenVerifier.class);
    adapter = new GoogleIdTokenVerifierAdapter(mockGoogleVerifier, "test-client-id");
  }

  @Test
  @DisplayName("idToken rỗng hoặc null phải ném ngoại lệ VALIDATION_FAILED")
  void verify_withBlankToken_shouldThrowValidationFailed() {
    assertThatThrownBy(() -> adapter.verify(null))
        .isInstanceOf(InvalidOperationException.class)
        .satisfies(ex -> assertThat(((InvalidOperationException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));

    assertThatThrownBy(() -> adapter.verify("   "))
        .isInstanceOf(InvalidOperationException.class)
        .satisfies(ex -> assertThat(((InvalidOperationException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  @DisplayName("Khi GoogleIdTokenVerifier trả về null (chữ ký sai hoặc hết hạn), phải ném UNAUTHORIZED")
  void verify_withInvalidSignatureOrExpired_shouldThrowUnauthorized() throws GeneralSecurityException, IOException {
    when(mockGoogleVerifier.verify("invalid.token")).thenReturn(null);

    assertThatThrownBy(() -> adapter.verify("invalid.token"))
        .isInstanceOf(InvalidOperationException.class)
        .satisfies(ex -> assertThat(((InvalidOperationException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED))
        .hasMessageContaining("không hợp lệ hoặc đã hết hạn");
  }

  @Test
  @DisplayName("Khi GoogleIdTokenVerifier xác thực thành công, trích xuất chính xác OAuthUserInfo")
  void verify_withValidToken_shouldReturnOAuthUserInfo() throws GeneralSecurityException, IOException {
    GoogleIdToken mockToken = mock(GoogleIdToken.class);
    GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
    payload.setSubject("google-sub-1001");
    payload.setEmail("student.verified@gmail.com");
    payload.set("name", "Nguyễn Văn Test");
    payload.set("picture", "https://google.com/avatar.jpg");

    when(mockToken.getPayload()).thenReturn(payload);
    when(mockGoogleVerifier.verify("valid.google.token")).thenReturn(mockToken);

    OAuthUserInfo result = adapter.verify("valid.google.token");

    assertThat(result).isNotNull();
    assertThat(result.provider()).isEqualTo("GOOGLE");
    assertThat(result.providerUserId()).isEqualTo("google-sub-1001");
    assertThat(result.email()).isEqualTo("student.verified@gmail.com");
    assertThat(result.fullName()).isEqualTo("Nguyễn Văn Test");
    assertThat(result.avatarUrl()).isEqualTo("https://google.com/avatar.jpg");
  }

  @Test
  @DisplayName("Khi Google SDK ném GeneralSecurityException, adapter phải bọc lại thành UNAUTHORIZED")
  void verify_whenGoogleSdkThrowsException_shouldWrapInUnauthorized() throws GeneralSecurityException, IOException {
    when(mockGoogleVerifier.verify("corrupted.token")).thenThrow(new GeneralSecurityException("Invalid key signature"));

    assertThatThrownBy(() -> adapter.verify("corrupted.token"))
        .isInstanceOf(InvalidOperationException.class)
        .satisfies(ex -> assertThat(((InvalidOperationException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED))
        .hasMessageContaining("Không thể xác thực tính hợp lệ");
  }
}

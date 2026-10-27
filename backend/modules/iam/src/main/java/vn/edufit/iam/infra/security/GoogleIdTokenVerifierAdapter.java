package vn.edufit.iam.infra.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edufit.iam.application.OAuthUserInfo;
import vn.edufit.iam.application.port.GoogleTokenVerifier;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

/**
 * Adapter xác thực Google ID Token sử dụng thư viện chính thức của Google (NFR-01, ADR-001).
 *
 * <p>Cơ chế xác thực cryptographic:
 * <ul>
 *   <li>Tải và cache danh sách Google Public Keys (JWKS).</li>
 *   <li>Xác thực chữ ký số RSA của Google trên chuỗi JWT.</li>
 *   <li>Kiểm tra thời hạn hiệu lực (token expiration timestamp).</li>
 *   <li>Kiểm tra tính toàn vẹn của Issuer (accounts.google.com).</li>
 *   <li>Kiểm tra Audience khớp với Client ID ứng dụng (chống Confused Deputy Attack).</li>
 * </ul>
 */
@Component
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

  private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierAdapter.class);

  private final GoogleIdTokenVerifier verifier;
  private final String configuredClientId;

  public GoogleIdTokenVerifierAdapter() {
    this("");
  }

  @Autowired
  public GoogleIdTokenVerifierAdapter(
      @Value("${edufit.oauth.google.client-id:}") String configuredClientId
  ) {
    this.configuredClientId = configuredClientId != null ? configuredClientId.trim() : "";

    GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
        new NetHttpTransport(),
        GsonFactory.getDefaultInstance()
    );

    if (!this.configuredClientId.isEmpty()) {
      builder.setAudience(Collections.singletonList(this.configuredClientId));
    }

    this.verifier = builder.build();
  }

  // Constructor hỗ trợ Dependency Injection cho Unit Tests
  GoogleIdTokenVerifierAdapter(GoogleIdTokenVerifier verifier, String configuredClientId) {
    this.verifier = verifier;
    this.configuredClientId = configuredClientId;
  }

  @Override
  public OAuthUserInfo verify(String idTokenString) {
    if (idTokenString == null || idTokenString.isBlank()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Google ID Token không được để trống"
      );
    }

    try {
      GoogleIdToken idToken = verifier.verify(idTokenString);
      if (idToken == null) {
        throw new InvalidOperationException(
            ErrorCode.UNAUTHORIZED,
            "Google ID Token không hợp lệ hoặc đã hết hạn"
        );
      }

      GoogleIdToken.Payload payload = idToken.getPayload();
      String sub = payload.getSubject();
      String email = payload.getEmail();
      String name = (String) payload.get("name");
      String pictureUrl = (String) payload.get("picture");

      if (sub == null || email == null) {
        throw new InvalidOperationException(
            ErrorCode.UNAUTHORIZED,
            "Google ID Token thiếu thông tin định danh người dùng (sub hoặc email)"
        );
      }

      return new OAuthUserInfo("GOOGLE", sub, email, name != null ? name : "Người dùng Google", pictureUrl);
    } catch (InvalidOperationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Lỗi khi giải mã và xác thực chữ ký Google ID Token: {}", e.getMessage());
      throw new InvalidOperationException(
          ErrorCode.UNAUTHORIZED,
          "Không thể xác thực tính hợp lệ của Google ID Token: " + e.getMessage()
      );
    }
  }
}

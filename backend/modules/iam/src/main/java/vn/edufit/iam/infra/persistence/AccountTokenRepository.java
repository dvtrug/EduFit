package vn.edufit.iam.infra.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository quản lý bảng {@code account_tokens}.
 */
@Repository
public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

  /**
   * Tìm kiếm token theo mã băm SHA-256 và loại mục đích.
   */
  Optional<AccountToken> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);

  /**
   * Lấy token mới nhất theo loại của tài khoản để kiểm tra thời gian giãn cách (cooldown 60s).
   */
  Optional<AccountToken> findTopByAccountAndTokenTypeOrderByCreatedAtDesc(Account account, TokenType tokenType);

  /**
   * Vô hiệu hóa toàn bộ các token cùng loại chưa dùng trước đó của tài khoản (dùng khi gửi lại mã resend).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE AccountToken t
      SET t.usedAt = :now
      WHERE t.account = :account
        AND t.tokenType = :tokenType
        AND t.usedAt IS NULL
      """)
  int invalidatePreviousTokens(
      @Param("account") Account account,
      @Param("tokenType") TokenType tokenType,
      @Param("now") Instant now
  );
}

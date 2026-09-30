package vn.edufit.iam.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository quản lý truy xuất bảng {@code accounts}.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

  /**
   * Tìm tài khoản theo email (đã chuẩn hóa lowercase).
   */
  Optional<Account> findByEmail(String email);

  /**
   * Kiểm tra sự tồn tại của email trong hệ thống (BR-01).
   */
  boolean existsByEmail(String email);
}

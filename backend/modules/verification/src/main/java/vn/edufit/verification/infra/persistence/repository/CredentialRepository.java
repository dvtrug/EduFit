package vn.edufit.verification.infra.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.verification.infra.persistence.entity.Credential;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, UUID> {
}

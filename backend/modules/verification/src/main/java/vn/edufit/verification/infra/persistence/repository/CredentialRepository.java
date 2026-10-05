package vn.edufit.verification.infra.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edufit.verification.infra.persistence.entity.Credential;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
}


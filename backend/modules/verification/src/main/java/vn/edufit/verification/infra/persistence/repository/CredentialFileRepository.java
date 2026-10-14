package vn.edufit.verification.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edufit.verification.infra.persistence.entity.CredentialFile;

@Repository
public interface CredentialFileRepository extends JpaRepository<CredentialFile, UUID> {

  @Query("""
      SELECT cf FROM CredentialFile cf
      JOIN FETCH cf.credential c
      JOIN FETCH c.verificationRequest vr
      WHERE cf.fileId = :fileId
  """)
  Optional<CredentialFile> findWithRequestByFileId(@Param("fileId") UUID fileId);
}

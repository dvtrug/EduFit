package vn.edufit.profile.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

  Optional<StudentProfile> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);
}

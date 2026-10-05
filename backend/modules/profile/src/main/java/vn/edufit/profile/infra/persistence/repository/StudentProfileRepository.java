package vn.edufit.profile.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

  Optional<StudentProfile> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);
}


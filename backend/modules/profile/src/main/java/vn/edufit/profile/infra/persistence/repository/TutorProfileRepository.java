package vn.edufit.profile.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;

public interface TutorProfileRepository extends JpaRepository<TutorProfile, UUID> {

  Optional<TutorProfile> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);
}


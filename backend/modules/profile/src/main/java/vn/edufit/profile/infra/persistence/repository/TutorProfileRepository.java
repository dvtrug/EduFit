package vn.edufit.profile.infra.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.entity.TutorStatus;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, UUID> {

  Optional<TutorProfile> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);

  List<TutorProfile> findByStatus(TutorStatus status);

  List<TutorProfile> findByTutorIdInAndStatus(Collection<UUID> tutorIds, TutorStatus status);

  @Query("""
      select t
      from TutorProfile t
      where t.status = :status
        and ((:subjectId is null and :levelId is null) or exists (
          select 1
          from TutorSubject ts
          where ts.tutorId = t.tutorId
            and (:subjectId is null or ts.subjectId = :subjectId)
            and (:levelId is null or ts.educationLevelId = :levelId)
        ))
        and (:area is null or lower(t.area) like lower(concat('%', :area, '%')))
        and (:mode is null or t.teachingMode = :mode or (:includeBothMode = true and t.teachingMode = :bothMode))
        and (:minPrice is null or t.pricePerSession >= :minPrice)
        and (:maxPrice is null or t.pricePerSession <= :maxPrice)
        and (:minRating is null or t.ratingAvg >= :minRating)
        and (:keyword is null
          or lower(t.displayName) like lower(concat('%', :keyword, '%'))
          or lower(t.headline) like lower(concat('%', :keyword, '%'))
          or lower(t.bio) like lower(concat('%', :keyword, '%'))
          or exists (
            select 1
            from TutorSubject keywordTs, Subject keywordSubject
            where keywordTs.tutorId = t.tutorId
              and keywordSubject.subjectId = keywordTs.subjectId
              and lower(keywordSubject.name) like lower(concat('%', :keyword, '%'))
          )
        )
        and (:dayOfWeek is null or exists (
          select 1
          from TutorAvailabilitySlot slot
          where slot.tutorId = t.tutorId
            and slot.dayOfWeek = :dayOfWeek
            and slot.startTime < :availableTo
            and slot.endTime > :availableFrom
        ))
      """)
  Page<TutorProfile> searchVerifiedTutors(
      @Param("status") TutorStatus status,
      @Param("subjectId") Integer subjectId,
      @Param("levelId") Integer levelId,
      @Param("area") String area,
      @Param("mode") TeachingMode mode,
      @Param("includeBothMode") boolean includeBothMode,
      @Param("bothMode") TeachingMode bothMode,
      @Param("minPrice") Long minPrice,
      @Param("maxPrice") Long maxPrice,
      @Param("minRating") BigDecimal minRating,
      @Param("keyword") String keyword,
      @Param("dayOfWeek") Short dayOfWeek,
      @Param("availableFrom") LocalTime availableFrom,
      @Param("availableTo") LocalTime availableTo,
      Pageable pageable
  );
}

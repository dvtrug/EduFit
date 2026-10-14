package vn.edufit.review.application.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.review.api.ReviewFacade;
import vn.edufit.review.api.dto.TutorRatingSummaryDto;
import vn.edufit.review.infra.persistence.entity.ReviewEntity;
import vn.edufit.review.infra.persistence.repository.ReviewRepository;

/**
 * Hiện thực hóa Public Facade của module Review (ADR-002).
 */
@Service
@Transactional(readOnly = true)
public class ReviewFacadeImpl implements ReviewFacade {

  private final ReviewRepository reviewRepository;

  public ReviewFacadeImpl(ReviewRepository reviewRepository) {
    this.reviewRepository = reviewRepository;
  }

  @Override
  public double getAverageRatingByTutorId(UUID tutorId) {
    if (tutorId == null) {
      return 0.0;
    }
    Double avg = reviewRepository.findAverageRatingByTutorId(tutorId);
    return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
  }

  @Override
  public int countReviewsByTutorId(UUID tutorId) {
    if (tutorId == null) {
      return 0;
    }
    return reviewRepository.countByTutorId(tutorId);
  }

  @Override
  public Optional<TutorRatingSummaryDto> getTutorRatingSummary(UUID tutorId) {
    if (tutorId == null) {
      return Optional.empty();
    }
    double avg = getAverageRatingByTutorId(tutorId);
    int count = countReviewsByTutorId(tutorId);
    return Optional.of(new TutorRatingSummaryDto(tutorId, avg, count));
  }

  @Override
  public Map<UUID, TutorRatingSummaryDto> getTutorRatingSummaries(Collection<UUID> tutorIds) {
    Map<UUID, TutorRatingSummaryDto> resultMap = new HashMap<>();
    if (tutorIds == null || tutorIds.isEmpty()) {
      return resultMap;
    }

    List<ReviewEntity> reviews = reviewRepository.findByTutorIdIn(tutorIds);
    Map<UUID, Long> countMap = new HashMap<>();
    Map<UUID, Long> sumMap = new HashMap<>();

    for (ReviewEntity review : reviews) {
      UUID tid = review.getTutorId();
      countMap.put(tid, countMap.getOrDefault(tid, 0L) + 1L);
      sumMap.put(tid, sumMap.getOrDefault(tid, 0L) + review.getRating());
    }

    for (UUID tutorId : tutorIds) {
      long count = countMap.getOrDefault(tutorId, 0L);
      long sum = sumMap.getOrDefault(tutorId, 0L);
      double avg = count > 0 ? Math.round(((double) sum / count) * 10.0) / 10.0 : 0.0;
      resultMap.put(tutorId, new TutorRatingSummaryDto(tutorId, avg, (int) count));
    }

    return resultMap;
  }

  @Override
  public boolean hasReviewed(UUID tutorId, UUID studentId) {
    if (tutorId == null || studentId == null) {
      return false;
    }
    return reviewRepository.existsByTutorIdAndStudentId(tutorId, studentId);
  }
}


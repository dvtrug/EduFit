package vn.edufit.review.web.response;

import java.time.Instant;
import java.util.UUID;
import vn.edufit.review.infra.persistence.entity.ReviewEntity;

/**
 * Response payload trả về cho giao diện Client thông tin một đánh giá.
 */
public record ReviewResponse(
    UUID reviewId,
    UUID tutorId,
    UUID studentId,
    UUID reviewerUserId,
    String reviewerName,
    String reviewerType,
    int rating,
    String comment,
    boolean isEdited,
    int completedSessionsAtReview,
    Instant createdAt,
    Instant updatedAt
) {

  public static ReviewResponse from(ReviewEntity entity, String reviewerName) {
    return new ReviewResponse(
        entity.getReviewId(),
        entity.getTutorId(),
        entity.getStudentId(),
        entity.getReviewerUserId(),
        reviewerName,
        entity.getReviewerType(),
        entity.getRating(),
        entity.getComment(),
        entity.isEdited(),
        entity.getCompletedSessionsAtReview(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}


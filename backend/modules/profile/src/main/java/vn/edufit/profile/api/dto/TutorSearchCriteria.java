package vn.edufit.profile.api.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Bộ tiêu chí tìm kiếm hồ sơ gia sư dùng bởi module Discovery qua public facade.
 */
public record TutorSearchCriteria(
    Integer subjectId,
    Integer levelId,
    String area,
    String mode,
    Long minPrice,
    Long maxPrice,
    BigDecimal minRating,
    String keyword,
    Short dayOfWeek,
    LocalTime availableFrom,
    LocalTime availableTo
) {

  public TutorSearchCriteria(
      Integer subjectId,
      Integer levelId,
      String area,
      String mode,
      Long minPrice,
      Long maxPrice,
      BigDecimal minRating
  ) {
    this(subjectId, levelId, area, mode, minPrice, maxPrice, minRating, null, null, null, null);
  }
}

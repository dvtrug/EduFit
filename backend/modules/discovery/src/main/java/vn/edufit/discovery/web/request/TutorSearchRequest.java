package vn.edufit.discovery.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import java.time.LocalTime;
import vn.edufit.profile.api.dto.TutorSearchCriteria;

public record TutorSearchRequest(
    @Min(value = 1, message = "subjectId phải lớn hơn 0")
    Integer subjectId,

    @Min(value = 1, message = "levelId phải lớn hơn 0")
    Integer levelId,

    String area,

    @Pattern(regexp = "(?i)ONLINE|OFFLINE|BOTH", message = "mode phải là ONLINE, OFFLINE hoặc BOTH")
    String mode,

    @Min(value = 0, message = "minPrice không được âm")
    Long minPrice,

    @Min(value = 0, message = "maxPrice không được âm")
    Long maxPrice,

    @DecimalMin(value = "0.0", message = "minRating không được âm")
    @DecimalMax(value = "5.0", message = "minRating không được lớn hơn 5")
    BigDecimal minRating,

    String keyword,

    @Min(value = 1, message = "dayOfWeek phải từ 1 đến 7")
    @jakarta.validation.constraints.Max(value = 7, message = "dayOfWeek phải từ 1 đến 7")
    Short dayOfWeek,

    LocalTime availableFrom,

    LocalTime availableTo
) {

  public TutorSearchRequest(
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

  @AssertTrue(message = "minPrice không được lớn hơn maxPrice")
  public boolean isPriceRangeValid() {
    return minPrice == null || maxPrice == null || minPrice <= maxPrice;
  }

  @AssertTrue(message = "dayOfWeek, availableFrom và availableTo phải được cung cấp cùng nhau")
  public boolean isAvailabilityComplete() {
    boolean none = dayOfWeek == null && availableFrom == null && availableTo == null;
    boolean all = dayOfWeek != null && availableFrom != null && availableTo != null;
    return none || all;
  }

  @AssertTrue(message = "availableTo phải sau availableFrom")
  public boolean isAvailabilityRangeValid() {
    return availableFrom == null || availableTo == null || availableTo.isAfter(availableFrom);
  }

  public TutorSearchCriteria toCriteria() {
    return new TutorSearchCriteria(
        subjectId, levelId, area, mode, minPrice, maxPrice, minRating,
        keyword, dayOfWeek, availableFrom, availableTo
    );
  }
}

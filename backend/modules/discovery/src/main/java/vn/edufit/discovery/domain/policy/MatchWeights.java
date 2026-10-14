package vn.edufit.discovery.domain.policy;

import java.math.BigDecimal;
import java.util.List;

public record MatchWeights(BigDecimal subject, BigDecimal level, BigDecimal price,
                           BigDecimal time, BigDecimal rating) {

  public MatchWeights {
    var values = List.of(subject, level, price, time, rating);
    if (values.stream().anyMatch(value -> value.signum() < 0)
        || values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ONE) != 0) {
      throw new IllegalArgumentException("Matching weights must be non-negative and sum to 1.");
    }
  }

  public static MatchWeights standard() {
    // Four criteria: learning need (subject + level) 40%, affordability 10%,
    // practical availability 25%, and review confidence 25%.
    return new MatchWeights(new BigDecimal("0.20"), new BigDecimal("0.20"),
        new BigDecimal("0.10"), new BigDecimal("0.25"), new BigDecimal("0.25"));
  }
}

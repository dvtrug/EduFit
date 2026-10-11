package vn.edufit.discovery.domain.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MatchWeightsTest {

  @ParameterizedTest
  @CsvSource({"-0.30,0.80,0.20,0.15,0.15", "0.30,0.20,0.20,0.15,0.14", "0.30,0.20,0.20,0.15,0.16"})
  void rejectsNegativeWeightsOrNonUnitSum(BigDecimal subject, BigDecimal level, BigDecimal price,
                                         BigDecimal time, BigDecimal rating) {
    assertThrows(IllegalArgumentException.class, () -> new MatchWeights(subject, level, price, time, rating));
  }

  @Test
  void standardWeightsMatchSpecificationAndZeroWeightsAreAllowed() {
    assertEquals(new MatchWeights(new BigDecimal("0.30"), new BigDecimal("0.20"), new BigDecimal("0.20"),
        new BigDecimal("0.15"), new BigDecimal("0.15")), MatchWeights.standard());
    assertDoesNotThrow(() -> new MatchWeights(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    assertThrows(NullPointerException.class, () -> new MatchWeights(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
  }
}

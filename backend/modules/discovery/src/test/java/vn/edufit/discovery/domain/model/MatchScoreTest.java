package vn.edufit.discovery.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchScoreTest {

  @Test
  void rankingUsesTotalTimeReviewsRegistrationAndIdInThatOrder() {
    var expected = List.of(
        score(6, "91", "0", 0, null),
        score(5, "90", "80", 0, null),
        score(4, "90", "70", 10, null),
        score(2, "90", "70", 5, Instant.EPOCH),
        score(3, "90", "70", 5, Instant.EPOCH),
        score(7, "90", "70", 5, Instant.EPOCH.plusSeconds(1)),
        score(1, "90", "70", 5, null));
    for (int seed = 0; seed < 20; seed++) {
      var shuffled = new ArrayList<>(expected);
      Collections.shuffle(shuffled, new Random(seed));
      assertEquals(expected, shuffled.stream().sorted(MatchScore.rankingOrder()).toList());
    }
  }

  private MatchScore score(long id, String total, String time, int reviews, Instant registered) {
    // Deliberately oppose slot count and time fit: ranking must use duration fit.
    return new MatchScore(new UUID(0, id), new BigDecimal(total), BigDecimal.valueOf(100), BigDecimal.valueOf(100),
        new BigDecimal(time), BigDecimal.ZERO, BigDecimal.ZERO, 100 - Integer.parseInt(time), reviews, registered);
  }
}

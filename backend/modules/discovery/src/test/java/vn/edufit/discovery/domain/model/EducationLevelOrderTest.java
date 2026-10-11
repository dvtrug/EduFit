package vn.edufit.discovery.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EducationLevelOrderTest {

  @ParameterizedTest
  @CsvSource({"80,3,true", "3,80,true", "3,41,true", "80,41,false", "41,41,false", "1,2,false", ",3,false"})
  void adjacencyUsesCatalogPositions(Integer first, Integer second, boolean expected) {
    var order = new EducationLevelOrder(List.of(80, 3, 41, 12));
    assertEquals(expected, order.areAdjacent(first, second));
  }

  @Test
  void orderIsImmutableAndRejectsDuplicateIds() {
    var source = new ArrayList<>(List.of(80, 3));
    var order = new EducationLevelOrder(source);
    source.clear();
    assertEquals(List.of(80, 3), order.levelIds());
    assertThrows(UnsupportedOperationException.class, () -> order.levelIds().add(41));
    assertThrows(IllegalArgumentException.class, () -> new EducationLevelOrder(List.of(3, 3)));
  }
}

package vn.edufit.discovery.domain.model;

import java.util.HashSet;
import java.util.List;

/** Level IDs ordered by the catalog's business order, not by their numeric values. */
public record EducationLevelOrder(List<Integer> levelIds) {

  public EducationLevelOrder {
    levelIds = List.copyOf(levelIds);
    if (new HashSet<>(levelIds).size() != levelIds.size()) {
      throw new IllegalArgumentException("Education level IDs must be unique.");
    }
  }

  public boolean areAdjacent(Integer firstLevelId, Integer secondLevelId) {
    if (firstLevelId == null || secondLevelId == null) {
      return false;
    }
    int first = levelIds.indexOf(firstLevelId);
    int second = levelIds.indexOf(secondLevelId);
    return first >= 0 && second >= 0 && Math.abs(first - second) == 1;
  }
}

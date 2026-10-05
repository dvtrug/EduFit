package vn.edufit.profile.web.response;

import java.util.List;

public record CatalogResponse(
    List<EducationLevelItem> educationLevels,
    List<SubjectItem> subjects
) {
  public record EducationLevelItem(
      Integer levelId,
      String name,
      Integer sortOrder
  ) {}

  public record SubjectItem(
      Integer subjectId,
      String name
  ) {}
}

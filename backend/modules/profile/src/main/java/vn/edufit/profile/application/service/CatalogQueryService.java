package vn.edufit.profile.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.web.response.CatalogResponse;

@Service
public class CatalogQueryService {

  private final EducationLevelRepository educationLevelRepository;
  private final SubjectRepository subjectRepository;

  public CatalogQueryService(
      EducationLevelRepository educationLevelRepository,
      SubjectRepository subjectRepository
  ) {
    this.educationLevelRepository = educationLevelRepository;
    this.subjectRepository = subjectRepository;
  }

  @Transactional(readOnly = true)
  public CatalogResponse getCatalog() {
    List<CatalogResponse.EducationLevelItem> levels = getEducationLevels();
    List<CatalogResponse.SubjectItem> subjects = getSubjects();
    return new CatalogResponse(levels, subjects);
  }

  @Transactional(readOnly = true)
  public List<CatalogResponse.EducationLevelItem> getEducationLevels() {
    return educationLevelRepository.findByIsActiveTrueOrderBySortOrderAsc()
        .stream()
        .map(level -> new CatalogResponse.EducationLevelItem(
            level.getLevelId(),
            level.getName(),
            level.getSortOrder()
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CatalogResponse.SubjectItem> getSubjects() {
    return subjectRepository.findByIsActiveTrueOrderByNameAsc()
        .stream()
        .map(subj -> new CatalogResponse.SubjectItem(
            subj.getSubjectId(),
            subj.getName()
        ))
        .toList();
  }
}

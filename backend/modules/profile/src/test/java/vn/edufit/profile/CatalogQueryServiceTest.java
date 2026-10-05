package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.profile.application.service.CatalogQueryService;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.Subject;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.web.response.CatalogResponse;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

  @Mock
  private EducationLevelRepository educationLevelRepository;

  @Mock
  private SubjectRepository subjectRepository;

  private CatalogQueryService catalogQueryService;

  @BeforeEach
  void setUp() {
    catalogQueryService = new CatalogQueryService(educationLevelRepository, subjectRepository);
  }

  @Test
  @DisplayName("Lấy danh mục cấp học và môn học thành công")
  void shouldReturnFullCatalog() {
    when(educationLevelRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(
        new EducationLevel("Lớp 10", 10, true),
        new EducationLevel("Lớp 11", 11, true)
    ));
    when(subjectRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(
        new Subject("Toán", true),
        new Subject("Vật lý", true)
    ));

    CatalogResponse catalog = catalogQueryService.getCatalog();

    assertNotNull(catalog);
    assertEquals(2, catalog.educationLevels().size());
    assertEquals(2, catalog.subjects().size());
    assertEquals("Lớp 10", catalog.educationLevels().get(0).name());
    assertEquals("Toán", catalog.subjects().get(0).name());
  }
}

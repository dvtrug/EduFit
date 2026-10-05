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
import org.springframework.http.ResponseEntity;
import vn.edufit.profile.application.service.CatalogQueryService;
import vn.edufit.profile.web.CatalogController;
import vn.edufit.profile.web.response.CatalogResponse;
import vn.edufit.shared.response.ApiResponse;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

  @Mock
  private CatalogQueryService catalogQueryService;

  private CatalogController catalogController;

  @BeforeEach
  void setUp() {
    catalogController = new CatalogController(catalogQueryService);
  }

  @Test
  @DisplayName("Lấy danh mục thành công qua CatalogController")
  void shouldGetCatalog() {
    CatalogResponse mockResponse = new CatalogResponse(
        List.of(new CatalogResponse.EducationLevelItem(1, "Lớp 12", 12)),
        List.of(new CatalogResponse.SubjectItem(1, "Toán"))
    );
    when(catalogQueryService.getCatalog()).thenReturn(mockResponse);

    ResponseEntity<ApiResponse<CatalogResponse>> resp = catalogController.getCatalog();
    assertNotNull(resp.getBody());
    assertEquals(1, resp.getBody().data().educationLevels().size());
  }

  @Test
  @DisplayName("Lấy danh sách cấp học qua CatalogController")
  void shouldGetEducationLevels() {
    List<CatalogResponse.EducationLevelItem> mockLevels = List.of(
        new CatalogResponse.EducationLevelItem(1, "Lớp 12", 12)
    );
    when(catalogQueryService.getEducationLevels()).thenReturn(mockLevels);

    ResponseEntity<ApiResponse<List<CatalogResponse.EducationLevelItem>>> resp =
        catalogController.getEducationLevels();
    assertNotNull(resp.getBody());
    assertEquals(1, resp.getBody().data().size());
  }

  @Test
  @DisplayName("Lấy danh sách môn học qua CatalogController")
  void shouldGetSubjects() {
    List<CatalogResponse.SubjectItem> mockSubjects = List.of(
        new CatalogResponse.SubjectItem(1, "Toán")
    );
    when(catalogQueryService.getSubjects()).thenReturn(mockSubjects);

    ResponseEntity<ApiResponse<List<CatalogResponse.SubjectItem>>> resp =
        catalogController.getSubjects();
    assertNotNull(resp.getBody());
    assertEquals(1, resp.getBody().data().size());
  }
}

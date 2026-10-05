package vn.edufit.profile.web;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edufit.profile.application.service.CatalogQueryService;
import vn.edufit.profile.web.response.CatalogResponse;
import vn.edufit.shared.response.ApiResponse;

/**
 * REST Controller cung cấp danh mục dùng chung (Cấp học, Môn học).
 */
@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {

  private final CatalogQueryService catalogQueryService;

  public CatalogController(CatalogQueryService catalogQueryService) {
    this.catalogQueryService = catalogQueryService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<CatalogResponse>> getCatalog() {
    CatalogResponse catalog = catalogQueryService.getCatalog();
    return ResponseEntity.ok(ApiResponse.success(catalog, "Lấy danh mục dữ liệu thành công"));
  }

  @GetMapping("/education-levels")
  public ResponseEntity<ApiResponse<List<CatalogResponse.EducationLevelItem>>> getEducationLevels() {
    List<CatalogResponse.EducationLevelItem> levels = catalogQueryService.getEducationLevels();
    return ResponseEntity.ok(ApiResponse.success(levels, "Lấy danh sách cấp học thành công"));
  }

  @GetMapping("/subjects")
  public ResponseEntity<ApiResponse<List<CatalogResponse.SubjectItem>>> getSubjects() {
    List<CatalogResponse.SubjectItem> subjects = catalogQueryService.getSubjects();
    return ResponseEntity.ok(ApiResponse.success(subjects, "Lấy danh sách môn học thành công"));
  }
}

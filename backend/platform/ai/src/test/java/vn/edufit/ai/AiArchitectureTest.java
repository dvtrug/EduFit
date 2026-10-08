package vn.edufit.ai;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Kiểm thử kiến trúc tự động bằng ArchUnit cho phân hệ AI Gateway.
 *
 * <p>Mục đích:
 * <ul>
 *   <li><b>Bảo vệ ranh giới đóng gói (Module Encapsulation):</b> Cưỡng chế các module khác
 *       chỉ được phép tương tác qua {@code vn.edufit.ai.api..}.</li>
 *   <li><b>Độc lập tầng API:</b> Tầng {@code api} không được phép phụ thuộc ngược lại vào
 *       {@code application} hay {@code infra} (nguyên tắc Ports & Adapters).</li>
 * </ul>
 */
@AnalyzeClasses(packages = "vn.edufit", importOptions = {ImportOption.DoNotIncludeTests.class})
public class AiArchitectureTest {

  /**
   * Quy tắc 1: Tầng API của module AI là cửa ngõ duy nhất.
   * Các class bên ngoài module AI không được phép gọi trực tiếp vào application hay infra của AI.
   */
  @ArchTest
  static final ArchRule external_modules_should_only_access_ai_api = noClasses()
      .that().resideOutsideOfPackage("vn.edufit.ai..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("vn.edufit.ai.application..", "vn.edufit.ai.infra..")
      .allowEmptyShould(true);

  /**
   * Quy tắc 2: Tầng API của AI phải độc lập, không được phụ thuộc vào application hay infra của chính nó.
   */
  @ArchTest
  static final ArchRule ai_api_must_not_depend_on_internal_layers = noClasses()
      .that().resideInAPackage("vn.edufit.ai.api..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("vn.edufit.ai.application..", "vn.edufit.ai.infra..")
      .allowEmptyShould(true);
}

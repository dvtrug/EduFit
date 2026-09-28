package vn.edufit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Kiểm thử kiến trúc tự động bằng ArchUnit (ADR-002).
 *
 * <p>Quy tắc:
 * <ul>
 *   <li>Cưỡng chế các ranh giới package kiến trúc ở mức kiểm thử đơn vị tự động.</li>
 *   <li>Ngăn ngừa việc các module gọi trực tiếp vào tầng nội bộ (domain/application/infra) của module khác.</li>
 * </ul>
 */
@AnalyzeClasses(packages = "vn.edufit", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

  // Quy tắc 1: Module khác không được truy cập trực tiếp vào domain/application/infra của module khác
  // Chỉ được phép gọi qua package ..api..
  @ArchTest
  static final ArchRule modules_should_only_interact_through_api = noClasses()
      .that().resideInAPackage("vn.edufit.modules.(*)..")
      .should().dependOnClassesThat()
      .resideInAnyPackage(
          "vn.edufit.modules.(*).application..",
          "vn.edufit.modules.(*).domain..",
          "vn.edufit.modules.(*).infra.."
      )
      .allowEmptyShould(true);

  // Quy tắc 2: Notification không được có bất kỳ module nào phụ thuộc vào nó (Notification chỉ nghe Event)
  @ArchTest
  static final ArchRule no_module_should_depend_on_notification = noClasses()
      .that().resideInAnyPackage(
          "vn.edufit.modules.iam..",
          "vn.edufit.modules.profile..",
          "vn.edufit.modules.scheduling..",
          "vn.edufit.modules.discovery..",
          "vn.edufit.modules.connection..",
          "vn.edufit.modules.progress..",
          "vn.edufit.modules.review.."
      )
      .should().dependOnClassesThat()
      .resideInAPackage("vn.edufit.modules.notification..")
      .allowEmptyShould(true);
}

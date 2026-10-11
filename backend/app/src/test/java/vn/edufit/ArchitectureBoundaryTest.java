package vn.edufit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Discovery's domain and interface contracts must not leak adapter dependencies. */
@AnalyzeClasses(packages = "vn.edufit", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureBoundaryTest {
  @ArchTest
  static final ArchRule discovery_domain_is_pure_java = classes()
      .that().resideInAPackage("vn.edufit.discovery.domain..")
      .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "vn.edufit.discovery.domain..")
      .allowEmptyShould(false);

  @ArchTest
  static final ArchRule discovery_application_does_not_depend_on_web = noClasses()
      .that().resideInAPackage("vn.edufit.discovery.application..")
      .should().dependOnClassesThat().resideInAPackage("vn.edufit.discovery.web..")
      .allowEmptyShould(false);

  @ArchTest
  static final ArchRule discovery_api_does_not_expose_implementation = classes()
      .that().resideInAPackage("vn.edufit.discovery.api..")
      .should().onlyDependOnClassesThat()
      .resideInAnyPackage("java..", "vn.edufit.discovery.api..", "vn.edufit.shared..")
      .allowEmptyShould(false);

  @ArchTest
  static final ArchRule outsiders_use_discovery_public_api = noClasses()
      .that().resideOutsideOfPackage("vn.edufit.discovery..")
      .should().dependOnClassesThat().resideInAnyPackage("vn.edufit.discovery.domain..",
          "vn.edufit.discovery.application..", "vn.edufit.discovery.infra..", "vn.edufit.discovery.web..")
      .allowEmptyShould(false);

  @ArchTest
  static final ArchRule discovery_uses_only_public_edufit_dependencies = classes()
      .that().resideInAPackage("vn.edufit.discovery..")
      .should().onlyDependOnClassesThat(new DescribedPredicate<JavaClass>("are own types, shared kernel or public module/platform APIs") {
        @Override public boolean test(JavaClass target) {
          String pkg = target.getPackageName();
          if (!pkg.startsWith("vn.edufit.")) return true;
          if (pkg.startsWith("vn.edufit.discovery.") || pkg.startsWith("vn.edufit.shared.")) return true;
          String[] parts = pkg.split("\\.");
          return parts.length >= 4 && parts[3].equals("api");
        }
      }).allowEmptyShould(false);

  @ArchTest
  static void production_import_contains_expected_layers(JavaClasses imported) {
    for (String name : new String[] {
        "vn.edufit.discovery.domain.policy.MatchScorer",
        "vn.edufit.discovery.application.service.TutorMatchingService",
        "vn.edufit.discovery.application.service.DiscoveryFacadeImpl",
        "vn.edufit.discovery.api.DiscoveryFacade",
        "vn.edufit.discovery.web.DiscoveryController",
        "vn.edufit.connection.application.ConnectionService",
        "vn.edufit.profile.application.service.ProfileFacadeImpl"}) {
      assertTrue(imported.contain(name), "Architecture import must include " + name);
    }
  }
}

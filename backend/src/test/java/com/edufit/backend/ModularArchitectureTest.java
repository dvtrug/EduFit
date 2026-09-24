package com.edufit.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularArchitectureTest {

  private static final Set<String> EXPECTED_MODULES =
      Set.of(
          "identity",
          "verification",
          "discovery",
          "relationship",
          "scheduling",
          "learning",
          "feedback",
          "notification");

  private final ApplicationModules modules = ApplicationModules.of(EdufitApplication.class);

  @Test
  void hasTheExpectedBusinessModules() {
    var detectedModules =
        modules.stream()
            .map(module -> module.getIdentifier().toString())
            .collect(Collectors.toSet());

    assertThat(detectedModules).containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
  }

  @Test
  void modulesDoNotContainCyclesOrIllegalDependencies() {
    modules.verify();
  }
}

package vn.edufit.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  private final OpenApiConfig config = new OpenApiConfig();

  @Test
  void documentsTheRuntimeSessionCookie() {
    var api = config.customOpenAPI();

    assertThat(api.getComponents().getSecuritySchemes().get("SessionCookieAuth").getName())
        .isEqualTo("EDUFIT_SESSION");
    assertThat(api.getSecurity()).containsExactly(api.getSecurity().getFirst());
    assertThat(api.getSecurity().getFirst()).containsKey("SessionCookieAuth");
  }

  @Test
  void removesGlobalSecurityFromPublicDiscoveryAndCatalogReadsOnly() {
    Operation publicSearch = securedOperation();
    Operation publicDetail = securedOperation();
    Operation publicCatalog = securedOperation();
    Operation publicLevels = securedOperation();
    Operation publicSubjects = securedOperation();
    Operation protectedMatch = securedOperation();
    OpenAPI api =
        config.customOpenAPI()
            .paths(
                new Paths()
                    .addPathItem("/api/v1/discovery/tutors", new PathItem().get(publicSearch))
                    .addPathItem(
                        "/api/v1/discovery/tutors/{tutorId}",
                        new PathItem().get(publicDetail))
                    .addPathItem("/api/v1/catalogs", new PathItem().get(publicCatalog))
                    .addPathItem(
                        "/api/v1/catalogs/education-levels", new PathItem().get(publicLevels))
                    .addPathItem("/api/v1/catalogs/subjects", new PathItem().get(publicSubjects))
                    .addPathItem("/api/v1/discovery/match", new PathItem().post(protectedMatch)));

    config.publicOperationSecurityCustomizer().customise(api);

    assertThat(publicSearch.getSecurity()).isEmpty();
    assertThat(publicDetail.getSecurity()).isEmpty();
    assertThat(publicCatalog.getSecurity()).isEmpty();
    assertThat(publicLevels.getSecurity()).isEmpty();
    assertThat(publicSubjects.getSecurity()).isEmpty();
    assertThat(protectedMatch.getSecurity()).isNotEmpty();
    assertThat(api.getSecurity()).isNotEmpty();
  }

  private Operation securedOperation() {
    return new Operation().security(List.of(new io.swagger.v3.oas.models.security.SecurityRequirement()
        .addList("SessionCookieAuth")));
  }
}

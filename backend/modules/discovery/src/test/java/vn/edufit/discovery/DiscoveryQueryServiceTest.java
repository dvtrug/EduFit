package vn.edufit.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edufit.discovery.application.service.DiscoveryQueryService;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.EducationLevelOrderDto;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSearchCriteria;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.api.dto.TutorSubjectDto;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;
import vn.edufit.shared.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DiscoveryQueryServiceTest {

  @Mock
  private ProfileFacade profileFacade;

  private DiscoveryQueryService discoveryQueryService;

  @Test
  void shouldMapCatalogOrderingToPureDomainModel() {
    when(profileFacade.findEducationLevelsForMatching()).thenReturn(List.of(
        new EducationLevelOrderDto(80, 10), new EducationLevelOrderDto(3, 30),
        new EducationLevelOrderDto(41, 60)
    ));
    var order = discoveryQueryService.getEducationLevelOrder();
    assertEquals(List.of(80, 3, 41), order.levelIds());
    assertEquals(true, order.areAdjacent(80, 3));
    assertEquals(false, order.areAdjacent(80, 41));
  }

  @BeforeEach
  void setUp() {
    discoveryQueryService = new DiscoveryQueryService(profileFacade);
  }

  @Test
  @DisplayName("Giữ summary khi không có chi tiết hydrate cho gia sư")
  void shouldFallBackToSummaryWhenDetailsAreMissing() {
    TutorSearchCriteria criteria = new TutorSearchCriteria(1, 2, "Hà Nội", "ONLINE", 100_000L, 300_000L, BigDecimal.valueOf(4));
    PageRequest pageable = PageRequest.of(0, 20);
    TutorSummaryDto tutor = tutor(UUID.randomUUID(), "VERIFIED");

    when(profileFacade.searchTutors(criteria, pageable)).thenReturn(new PageImpl<>(java.util.List.of(tutor)));

    Page<TutorDiscoveryProfileDto> result = discoveryQueryService.searchTutors(criteria, pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals(tutor, result.getContent().getFirst().tutor());
    assertEquals(java.util.List.of(), result.getContent().getFirst().subjects());
  }

  @Test
  void shouldReturnHydratedProfilesAndPreservePageMetadata() {
    var criteria = new TutorSearchCriteria(null, null, null, null, null, null, null);
    var pageable = PageRequest.of(1, 10);
    var summary = tutor(UUID.randomUUID(), "VERIFIED");
    var hydrated = new TutorDiscoveryProfileDto(
        summary, List.of(new TutorSubjectDto(1, "Math", 2, "Secondary")),
        List.of(new WeeklyAvailabilityDto((short) 1, LocalTime.of(18, 0), LocalTime.of(20, 0))),
        Instant.parse("2025-01-01T00:00:00Z")
    );
    when(profileFacade.searchTutors(criteria, pageable))
        .thenReturn(new PageImpl<>(List.of(summary), pageable, 25));
    when(profileFacade.findVerifiedTutorDetails(List.of(summary.tutorId())))
        .thenReturn(List.of(hydrated));

    var result = discoveryQueryService.searchTutors(criteria, pageable);

    assertSame(hydrated, result.getContent().getFirst());
    assertEquals(25, result.getTotalElements());
    assertEquals(pageable, result.getPageable());
  }

  @Test
  void shouldSearchWithoutFiltersWhenCriteriaAreNull() {
    var pageable = PageRequest.of(0, 10);
    var criteria = new TutorSearchCriteria(null, null, null, null, null, null, null);
    when(profileFacade.searchTutors(criteria, pageable)).thenReturn(Page.empty(pageable));

    var result = discoveryQueryService.searchTutors(null, pageable);

    assertEquals(0, result.getTotalElements());
    assertEquals(pageable, result.getPageable());
  }

  @Test
  @DisplayName("Không trả chi tiết gia sư nếu hồ sơ chưa VERIFIED")
  void shouldRejectUnverifiedTutorDetail() {
    UUID tutorId = UUID.randomUUID();
    when(profileFacade.findVerifiedTutorDetail(tutorId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> discoveryQueryService.getTutorDetail(tutorId));
  }

  private TutorSummaryDto tutor(UUID tutorId, String status) {
    return new TutorSummaryDto(
        tutorId,
        UUID.randomUUID(),
        "Gia sư A",
        "Luyện thi Toán",
        "Bio nội bộ không trả ở card",
        "ONLINE",
        "Hà Nội",
        200_000L,
        (short) 5,
        "Phương pháp",
        status,
        Instant.now(),
        BigDecimal.valueOf(4.8),
        12
    );
  }
}

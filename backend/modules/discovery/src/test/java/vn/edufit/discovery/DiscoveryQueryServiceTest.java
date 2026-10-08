package vn.edufit.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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
import vn.edufit.discovery.web.request.TutorSearchRequest;
import vn.edufit.discovery.web.response.TutorDiscoveryCardResponse;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.shared.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DiscoveryQueryServiceTest {

  @Mock
  private ProfileFacade profileFacade;

  private DiscoveryQueryService discoveryQueryService;

  @BeforeEach
  void setUp() {
    discoveryQueryService = new DiscoveryQueryService(profileFacade);
  }

  @Test
  @DisplayName("Tìm kiếm gia sư trả về card discovery đã ẩn các trường nội bộ")
  void shouldSearchTutorCards() {
    TutorSearchRequest request = new TutorSearchRequest(1, 2, "Hà Nội", "ONLINE", 100_000L, 300_000L, BigDecimal.valueOf(4));
    PageRequest pageable = PageRequest.of(0, 20);
    TutorSummaryDto tutor = tutor(UUID.randomUUID(), "VERIFIED");

    when(profileFacade.searchTutors(request.toCriteria(), pageable)).thenReturn(new PageImpl<>(java.util.List.of(tutor)));

    Page<TutorDiscoveryCardResponse> result = discoveryQueryService.searchTutors(request, pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals(tutor.tutorId(), result.getContent().getFirst().tutorId());
    assertEquals("Gia sư A", result.getContent().getFirst().displayName());
    assertEquals(true, result.getContent().getFirst().verified());
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

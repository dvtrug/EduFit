package vn.edufit.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.discovery.application.service.MatchExplanationService;
import vn.edufit.discovery.application.service.TutorMatchingService;
import vn.edufit.discovery.infra.persistence.MatchingRunLog;
import vn.edufit.discovery.infra.persistence.MatchingRunLogRepository;
import vn.edufit.discovery.web.request.TutorMatchRequest;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.LearningGoalDiscoveryDto;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSubjectDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ForbiddenOperationException;

@ExtendWith(MockitoExtension.class)
class TutorMatchingServiceTest {

  @Mock
  private ProfileFacade profileFacade;
  @Mock
  private MatchingRunLogRepository logRepository;
  @Mock
  private MatchExplanationService explanationService;

  private TutorMatchingService matchingService;
  private UUID userId;
  private UUID goalId;
  private WeeklyAvailabilityDto monday;

  @BeforeEach
  void setUp() {
    matchingService = new TutorMatchingService(profileFacade, logRepository, explanationService);
    userId = UUID.randomUUID();
    goalId = UUID.randomUUID();
    monday = new WeeklyAvailabilityDto((short) 1, LocalTime.of(18, 0), LocalTime.of(20, 0));
  }

  @Test
  @DisplayName("Xếp hạng gia sư đủ hard filter, tạo giải thích và ghi matching run")
  void shouldRankEligibleTutorsAndLogRun() {
    TutorDiscoveryProfileDto best = tutor(BigDecimal.valueOf(5), 20, 250_000L);
    TutorDiscoveryProfileDto second = tutor(BigDecimal.valueOf(4), 8, 320_000L);
    when(profileFacade.findLearningGoalForDiscovery(goalId)).thenReturn(Optional.of(goal(userId)));
    when(profileFacade.findVerifiedTutorsForMatching()).thenReturn(List.of(second, best));
    when(explanationService.explainTopMatch(eq(userId), any(), eq(best)))
        .thenReturn(new MatchExplanationService.Explanation("Giải thích AI", true));
    when(explanationService.ruleBased(any())).thenReturn("Giải thích theo quy tắc");

    var result = matchingService.match(currentUser(userId), new TutorMatchRequest(goalId, 5));

    assertEquals(2, result.size());
    assertEquals(best.tutor().tutorId(), result.getFirst().tutorId());
    assertEquals(true, result.getFirst().aiGenerated());
    assertEquals("Giải thích theo quy tắc", result.get(1).explanation());
    verify(logRepository).save(any(MatchingRunLog.class));
  }

  @Test
  @DisplayName("Từ chối ghép đôi goal không thuộc người dùng hiện tại")
  void shouldRejectForeignGoal() {
    when(profileFacade.findLearningGoalForDiscovery(goalId)).thenReturn(Optional.of(goal(UUID.randomUUID())));

    assertThrows(
        ForbiddenOperationException.class,
        () -> matchingService.match(currentUser(userId), new TutorMatchRequest(goalId, 5))
    );
  }

  @Test
  @DisplayName("Từ chối role ngoài STUDENT/PARENT trước khi đọc goal")
  void shouldRejectNonLearnerRole() {
    assertThrows(
        ForbiddenOperationException.class,
        () -> matchingService.match(currentUser(userId, "TUTOR"), new TutorMatchRequest(goalId, 5))
    );
    org.mockito.Mockito.verifyNoInteractions(profileFacade);
  }

  private LearningGoalDiscoveryDto goal(UUID ownerId) {
    return new LearningGoalDiscoveryDto(
        goalId, UUID.randomUUID(), ownerId, 1, 2, "ONLINE", "Hà Nội",
        100_000L, 300_000L, "ACTIVE", List.of(monday)
    );
  }

  private TutorDiscoveryProfileDto tutor(BigDecimal rating, int reviews, long price) {
    UUID tutorId = UUID.randomUUID();
    TutorSummaryDto summary = new TutorSummaryDto(
        tutorId, UUID.randomUUID(), "Gia sư", "Toán", "Bio", "ONLINE", "Hà Nội",
        price, (short) 5, "Thực hành", "VERIFIED", Instant.now(), rating, reviews
    );
    return new TutorDiscoveryProfileDto(
        summary,
        List.of(new TutorSubjectDto(1, "Toán", 2, "THCS")),
        List.of(monday),
        Instant.parse("2025-01-01T00:00:00Z")
    );
  }

  private CurrentUser currentUser(UUID id) {
    return currentUser(id, "STUDENT");
  }

  private CurrentUser currentUser(UUID id, String role) {
    return new CurrentUser() {
      @Override
      public UUID getUserId() {
        return id;
      }

      @Override
      public String getEmail() {
        return "student@example.com";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of(role);
      }

      @Override
      public boolean hasRole(String role) {
        return getRoles().contains(role);
      }
    };
  }
}

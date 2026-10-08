package vn.edufit.discovery.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.discovery.domain.model.AvailabilitySlot;
import vn.edufit.discovery.domain.model.MatchCriteria;
import vn.edufit.discovery.domain.model.MatchScore;
import vn.edufit.discovery.domain.model.SubjectLevel;
import vn.edufit.discovery.domain.model.TutorCandidate;
import vn.edufit.discovery.domain.service.MatchScorer;
import vn.edufit.discovery.infra.persistence.MatchingRunLog;
import vn.edufit.discovery.infra.persistence.MatchingRunLogRepository;
import vn.edufit.discovery.web.request.TutorMatchRequest;
import vn.edufit.discovery.web.response.MatchScoreBreakdownResponse;
import vn.edufit.discovery.web.response.TutorDiscoveryCardResponse;
import vn.edufit.discovery.web.response.TutorMatchResponse;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.LearningGoalDiscoveryDto;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class TutorMatchingService {

  private final ProfileFacade profileFacade;
  private final MatchingRunLogRepository matchingRunLogRepository;
  private final MatchExplanationService explanationService;
  private final MatchScorer matchScorer = new MatchScorer();

  public TutorMatchingService(
      ProfileFacade profileFacade,
      MatchingRunLogRepository matchingRunLogRepository,
      MatchExplanationService explanationService
  ) {
    this.profileFacade = profileFacade;
    this.matchingRunLogRepository = matchingRunLogRepository;
    this.explanationService = explanationService;
  }

  @Transactional
  public List<TutorMatchResponse> match(CurrentUser currentUser, TutorMatchRequest request) {
    LearningGoalDiscoveryDto goal = profileFacade.findLearningGoalForDiscovery(request.goalId())
        .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", request.goalId()));
    validateAccess(currentUser, goal);
    MatchCriteria criteria = toCriteria(goal);

    List<TutorDiscoveryProfileDto> profiles = profileFacade.findVerifiedTutorsForMatching();
    Map<UUID, TutorDiscoveryProfileDto> byTutorId = profiles.stream()
        .collect(Collectors.toMap(profile -> profile.tutor().tutorId(), Function.identity()));
    List<MatchScore> ranked = profiles.stream()
        .map(this::toCandidate)
        .map(candidate -> matchScorer.score(criteria, candidate))
        .flatMap(java.util.Optional::stream)
        .sorted(MatchScore.rankingOrder())
        .limit(request.resolvedTopN())
        .toList();

    List<TutorMatchResponse> response = ranked.stream()
        .map(score -> toResponse(currentUser.getUserId(), score, byTutorId.get(score.tutorId()), score.equals(ranked.getFirst())))
        .toList();
    matchingRunLogRepository.save(new MatchingRunLog(
        currentUser.getUserId(), goal.studentId(), goal.goalId(), response.size()
    ));
    return response;
  }

  private void validateAccess(CurrentUser currentUser, LearningGoalDiscoveryDto goal) {
    if (!goal.studentUserId().equals(currentUser.getUserId())) {
      throw new ForbiddenOperationException("Bạn không có quyền ghép đôi cho mục tiêu học tập này.");
    }
    if (!"ACTIVE".equals(goal.status())) {
      throw new InvalidOperationException("Chỉ có thể ghép đôi với mục tiêu học tập đang hoạt động.");
    }
    if (goal.educationLevelId() == null) {
      throw new InvalidOperationException("Hồ sơ học sinh cần có cấp học trước khi ghép đôi.");
    }
    if (goal.budgetMax() == null || goal.budgetMax() <= 0) {
      throw new InvalidOperationException("Mục tiêu học tập cần có ngân sách tối đa lớn hơn 0.");
    }
    if (goal.availabilitySlots().isEmpty()) {
      throw new InvalidOperationException("Mục tiêu học tập cần ít nhất một khung giờ mong muốn.");
    }
  }

  private MatchCriteria toCriteria(LearningGoalDiscoveryDto goal) {
    return new MatchCriteria(
        goal.subjectId(), goal.educationLevelId(), goal.mode(), goal.area(), goal.budgetMin(),
        goal.budgetMax(), goal.availabilitySlots().stream().map(this::toSlot).toList()
    );
  }

  private TutorCandidate toCandidate(TutorDiscoveryProfileDto profile) {
    Set<SubjectLevel> subjects = profile.subjects().stream()
        .map(item -> new SubjectLevel(item.subjectId(), item.educationLevelId()))
        .collect(Collectors.toUnmodifiableSet());
    var tutor = profile.tutor();
    return new TutorCandidate(
        tutor.tutorId(), subjects, tutor.teachingMode(), tutor.area(), tutor.pricePerSession(),
        tutor.ratingAvg(), tutor.reviewCount() == null ? 0 : tutor.reviewCount(),
        profile.registeredAt(), profile.availabilitySlots().stream().map(this::toSlot).toList()
    );
  }

  private AvailabilitySlot toSlot(WeeklyAvailabilityDto slot) {
    return new AvailabilitySlot(slot.dayOfWeek(), slot.startTime(), slot.endTime());
  }

  private TutorMatchResponse toResponse(
      UUID userId,
      MatchScore score,
      TutorDiscoveryProfileDto profile,
      boolean topMatch
  ) {
    MatchExplanationService.Explanation explanation = topMatch
        ? explanationService.explainTopMatch(userId, score, profile)
        : new MatchExplanationService.Explanation(explanationService.ruleBased(score), false);
    return new TutorMatchResponse(
        score.tutorId(),
        score.total(),
        new MatchScoreBreakdownResponse(
            score.scheduleFit(), score.ratingFit(), score.budgetFit(), score.overlappingSlots()
        ),
        explanation.text(),
        explanation.aiGenerated(),
        TutorDiscoveryCardResponse.from(profile)
    );
  }
}

package vn.edufit.discovery.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.discovery.api.event.MatchingExecutedEvent;
import vn.edufit.discovery.application.dto.TutorMatchResult;
import vn.edufit.discovery.domain.model.AvailabilitySlot;
import vn.edufit.discovery.domain.model.MatchCriteria;
import vn.edufit.discovery.domain.model.MatchScore;
import vn.edufit.discovery.domain.model.SubjectLevel;
import vn.edufit.discovery.domain.model.TutorCandidate;
import vn.edufit.discovery.domain.policy.MatchScorer;
import vn.edufit.discovery.infra.persistence.entity.MatchingRunLogEntity;
import vn.edufit.discovery.infra.persistence.repository.MatchingRunLogRepository;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.LearningGoalDiscoveryDto;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorCandidateCriteria;
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
  private final DiscoveryQueryService queryService;
  private final ConnectionFacade connectionFacade;
  private final ApplicationEventPublisher eventPublisher;

  public TutorMatchingService(
      ProfileFacade profileFacade,
      MatchingRunLogRepository matchingRunLogRepository,
      MatchExplanationService explanationService,
      DiscoveryQueryService queryService,
      ConnectionFacade connectionFacade,
      ApplicationEventPublisher eventPublisher
  ) {
    this.profileFacade = profileFacade;
    this.matchingRunLogRepository = matchingRunLogRepository;
    this.explanationService = explanationService;
    this.queryService = queryService;
    this.connectionFacade = connectionFacade;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public List<TutorMatchResult> match(CurrentUser currentUser, UUID goalId, int topN) {
    if (currentUser == null || currentUser.getUserId() == null
        || (!currentUser.hasRole("STUDENT") && !currentUser.hasRole("PARENT"))) {
      throw new ForbiddenOperationException("Chỉ học sinh hoặc phụ huynh được yêu cầu ghép đôi gia sư.");
    }
    if (goalId == null || topN < 1 || topN > 10) {
      throw new InvalidOperationException("goalId là bắt buộc và topN phải từ 1 đến 10.");
    }
    LearningGoalDiscoveryDto goal = profileFacade.findLearningGoalForDiscovery(goalId)
        .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", goalId));
    validateAccess(currentUser, goal);
    MatchCriteria criteria = toCriteria(goal);
    var matchScorer = new MatchScorer(queryService.getEducationLevelOrder());

    List<TutorDiscoveryProfileDto> profiles = profileFacade.findVerifiedCandidatesBySubject(
        new TutorCandidateCriteria(goal.subjectId(), goal.mode(), goal.area()));
    Map<UUID, TutorDiscoveryProfileDto> byTutorId = profiles.stream()
        .collect(Collectors.toMap(profile -> profile.tutor().tutorId(), Function.identity()));
    List<MatchScore> ranked = profiles.stream()
        .map(this::toCandidate)
        .map(candidate -> matchScorer.score(criteria, candidate))
        .flatMap(java.util.Optional::stream)
        .sorted(MatchScore.rankingOrder())
        .limit(topN)
        .toList();

    List<TutorMatchResult> results = ranked.stream()
        .map(score -> toResult(currentUser.getUserId(), score, byTutorId.get(score.tutorId()), score.equals(ranked.getFirst())))
        .toList();
    var log = new MatchingRunLogEntity(
        currentUser.getUserId(), goal.studentId(), goal.goalId(), results.size()
    );
    matchingRunLogRepository.save(log);
    eventPublisher.publishEvent(new MatchingExecutedEvent(
        log.getUserId(), log.getStudentId(), log.getGoalId(), log.getResultCount(), log.getCreatedAt()));
    return results;
  }

  private void validateAccess(CurrentUser currentUser, LearningGoalDiscoveryDto goal) {
    boolean owner = currentUser.hasRole("STUDENT") && currentUser.getUserId().equals(goal.studentUserId());
    boolean linkedParent = !owner && currentUser.hasRole("PARENT")
        && connectionFacade.hasConfirmedParentLink(currentUser.getUserId(), goal.studentId());
    if (!owner && !linkedParent) {
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

  private TutorMatchResult toResult(
      UUID userId,
      MatchScore score,
      TutorDiscoveryProfileDto profile,
      boolean topMatch
  ) {
    MatchExplanationService.Explanation explanation = topMatch
        ? explanationService.explainTopMatch(userId, score, profile)
        : new MatchExplanationService.Explanation(explanationService.ruleBased(score), false);
    return new TutorMatchResult(score, profile, explanation.text(), explanation.aiGenerated());
  }
}

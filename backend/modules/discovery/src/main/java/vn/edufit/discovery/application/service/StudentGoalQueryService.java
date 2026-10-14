package vn.edufit.discovery.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.LearningGoalView;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ForbiddenOperationException;

/** Authorizes at Discovery's module boundary before Profile reads any student's goals. */
@Service
public class StudentGoalQueryService {
  private final ProfileFacade profiles;
  private final ConnectionFacade connections;

  public StudentGoalQueryService(ProfileFacade profiles, ConnectionFacade connections) {
    this.profiles = profiles;
    this.connections = connections;
  }

  @Transactional(readOnly = true)
  public List<LearningGoalView> list(CurrentUser actor, UUID studentId) {
    if (actor == null
        || actor.getUserId() == null
        || !actor.hasRole("PARENT")
        || !connections.hasConfirmedParentLink(actor.getUserId(), studentId))
      throw new ForbiddenOperationException(
          "Chỉ phụ huynh có liên kết xác nhận được đọc mục tiêu học tập.");
    return profiles.findLearningGoalsForStudent(studentId);
  }
}

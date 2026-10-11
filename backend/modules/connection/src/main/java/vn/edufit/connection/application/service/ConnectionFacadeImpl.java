package vn.edufit.connection.application.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository;
import vn.edufit.connection.infra.persistence.repository.TutoringClassRepository;

@Service
@Transactional(readOnly = true)
public class ConnectionFacadeImpl implements ConnectionFacade {
  private final ParentStudentLinkRepository links;
  private final TutoringClassRepository classes;

  public ConnectionFacadeImpl(ParentStudentLinkRepository links, TutoringClassRepository classes) {
    this.links = links;
    this.classes = classes;
  }

  @Override
  public boolean hasConfirmedParentLink(UUID parentUserId, UUID studentId) {
    return links.existsByParentUserIdAndStudentIdAndStatus(parentUserId, studentId, "CONFIRMED");
  }

  @Override
  public Optional<ActiveClassView> findActiveClass(UUID studentId, UUID tutorId) {
    return classes.findByStudentIdAndTutorIdAndStatus(studentId, tutorId, "ACTIVE")
        .map(c -> new ActiveClassView(c.getId(), c.getStudentId(), c.getTutorId(), c.getGoalId()));
  }
}

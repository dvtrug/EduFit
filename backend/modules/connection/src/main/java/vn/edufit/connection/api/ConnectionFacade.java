package vn.edufit.connection.api;

import java.util.Optional;
import java.util.UUID;

public interface ConnectionFacade {
  boolean hasConfirmedParentLink(UUID parentUserId, UUID studentId);
  Optional<ActiveClassView> findActiveClass(UUID studentId, UUID tutorId);
  boolean isClassActive(UUID classId);
  boolean isClassActive(UUID classId, UUID tutorId, UUID studentId);
  Optional<ActiveClassView> findActiveClassById(UUID classId);

  record ActiveClassView(UUID classId, UUID studentId, UUID tutorId, UUID goalId) {}
}

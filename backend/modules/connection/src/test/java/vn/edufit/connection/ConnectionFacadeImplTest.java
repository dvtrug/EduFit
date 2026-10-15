package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.application.service.ConnectionFacadeImpl;
import vn.edufit.connection.infra.persistence.entity.ConnectionRequest;
import vn.edufit.connection.infra.persistence.entity.TutoringClass;
import vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository;
import vn.edufit.connection.infra.persistence.repository.TutoringClassRepository;

class ConnectionFacadeImplTest {
  private final ParentStudentLinkRepository links = mock(ParentStudentLinkRepository.class);
  private final TutoringClassRepository classes = mock(TutoringClassRepository.class);
  private final ConnectionFacade facade = new ConnectionFacadeImpl(links, classes);

  @ParameterizedTest
  @ValueSource(strings = {"CONFIRMED", "PENDING", "DECLINED", "REVOKED", "NONE"})
  void matchingAuthorizationQueriesOnlyConfirmedLinks(String status) {
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID();
    when(links.existsByParentUserIdAndStudentIdAndStatus(parent, student, "CONFIRMED"))
        .thenReturn("CONFIRMED".equals(status));
    assertEquals("CONFIRMED".equals(status), facade.hasConfirmedParentLink(parent, student));
    verify(links).existsByParentUserIdAndStudentIdAndStatus(parent, student, "CONFIRMED");
    verifyNoMoreInteractions(links);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACTIVE", "ENDED", "NONE"})
  void activeClassLookupReturnsOnlyActiveDetachedView(String status) {
    UUID student = UUID.randomUUID(), tutor = UUID.randomUUID(), goal = UUID.randomUUID();
    var request = new ConnectionRequest(student, tutor, goal, UUID.randomUUID(), List.of("Tue 19:00"), null, Instant.now());
    var tutoringClass = new TutoringClass(request, Instant.now());
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(tutoringClass, "id", id);
    when(classes.findByStudentIdAndTutorIdAndStatus(student, tutor, "ACTIVE"))
        .thenReturn("ACTIVE".equals(status) ? Optional.of(tutoringClass) : Optional.empty());
    assertEquals("ACTIVE".equals(status) ? Optional.of(new ConnectionFacade.ActiveClassView(id, student, tutor, goal))
        : Optional.empty(), facade.findActiveClass(student, tutor));
    verify(classes).findByStudentIdAndTutorIdAndStatus(student, tutor, "ACTIVE");
    verifyNoMoreInteractions(classes);
  }

  @Test
  void contextResolvesExactlyOnePublicFacadeBean() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(ParentStudentLinkRepository.class, () -> links);
      context.registerBean(TutoringClassRepository.class, () -> classes);
      context.register(ConnectionFacadeImpl.class);
      context.refresh();
      assertEquals(1, context.getBeansOfType(ConnectionFacade.class).size());
      assertInstanceOf(ConnectionFacadeImpl.class, context.getBean(ConnectionFacade.class));
    }
  }

  @Test
  void isClassActiveSingleArgReturnsCorrectBoolean() {
    UUID activeId = UUID.randomUUID();
    UUID inactiveId = UUID.randomUUID();
    when(classes.existsByIdAndStatus(activeId, "ACTIVE")).thenReturn(true);
    when(classes.existsByIdAndStatus(inactiveId, "ACTIVE")).thenReturn(false);

    assertTrue(facade.isClassActive(activeId));
    assertFalse(facade.isClassActive(inactiveId));
    assertFalse(facade.isClassActive(null));
  }

  @Test
  void isClassActiveThreeArgsChecksClassTutorAndStudent() {
    UUID classId = UUID.randomUUID();
    UUID tutorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();

    when(classes.existsByIdAndTutorIdAndStudentIdAndStatus(classId, tutorId, studentId, "ACTIVE")).thenReturn(true);

    assertTrue(facade.isClassActive(classId, tutorId, studentId));
    assertFalse(facade.isClassActive(classId, tutorId, UUID.randomUUID()));
    assertFalse(facade.isClassActive(null, tutorId, studentId));
    assertFalse(facade.isClassActive(classId, null, studentId));
    assertFalse(facade.isClassActive(classId, tutorId, null));
  }

  @Test
  void findActiveClassByIdReturnsActiveViewWhenPresent() {
    UUID classId = UUID.randomUUID();
    UUID tutorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID goalId = UUID.randomUUID();

    var request = new ConnectionRequest(studentId, tutorId, goalId, UUID.randomUUID(), List.of("Tue 19:00"), null, Instant.now());
    var tutoringClass = new TutoringClass(request, Instant.now());
    ReflectionTestUtils.setField(tutoringClass, "id", classId);

    when(classes.findByIdAndStatus(classId, "ACTIVE")).thenReturn(Optional.of(tutoringClass));
    when(classes.findByIdAndStatus(UUID.randomUUID(), "ACTIVE")).thenReturn(Optional.empty());

    var result = facade.findActiveClassById(classId);
    assertTrue(result.isPresent());
    assertEquals(classId, result.get().classId());
    assertEquals(studentId, result.get().studentId());
    assertEquals(tutorId, result.get().tutorId());
    assertEquals(goalId, result.get().goalId());

    assertTrue(facade.findActiveClassById(null).isEmpty());
  }
}

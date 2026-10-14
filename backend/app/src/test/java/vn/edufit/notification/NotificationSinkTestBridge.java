package vn.edufit.notification;

import static org.mockito.Mockito.*;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edufit.shared.notification.NotificationSink;

/** Reuses the real listener/writer while the existing failure tests control their required sink. */
@TestConfiguration(proxyBeanMethods = false)
@Import({ConnectionTransitionListener.class, ConnectionNotificationWriter.class})
public class NotificationSinkTestBridge {
  @Bean NotificationRepository notificationRepository(NotificationSink sink) {
    var repository = mock(NotificationRepository.class);
    when(repository.save(any(Notification.class))).thenAnswer(call -> {
      Notification row = call.getArgument(0);
      sink.notify((UUID) field(row, "userId"), (UUID) field(row, "studentId"), (String) field(row, "type"),
          (String) field(row, "title"), (String) field(row, "text"), (String) field(row, "targetType"), (UUID) field(row, "targetId"));
      return row;
    });
    return repository;
  }
  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(vn.edufit.connection.api.ConnectionFacade.class)
  vn.edufit.connection.api.ConnectionFacade connectionFacade() {
    return mock(vn.edufit.connection.api.ConnectionFacade.class);
  }
  private Object field(Notification row, String name) { return ReflectionTestUtils.getField(row, name); }
}

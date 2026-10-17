package vn.edufit.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Imports the production consumer with a real repository supplied by each database fixture. */
@TestConfiguration(proxyBeanMethods = false)
@Import({ConnectionTransitionListener.class,ConnectionNotificationWriter.class})
public class ConnectionNotificationTestConfig {}

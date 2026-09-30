package vn.edufit.iam.infra.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import vn.edufit.iam.application.port.AccountEmailSender;

/**
 * Adapter gửi email giả lập qua Logger phục vụ môi trường Dev và Test (NFR-11).
 */
@Component
@ConditionalOnMissingBean(type = "vn.edufit.iam.infra.mail.RealSmtpEmailSender")
public class LoggingAccountEmailSender implements AccountEmailSender {

  private static final Logger log = LoggerFactory.getLogger(LoggingAccountEmailSender.class);

  @Override
  public void sendPasswordResetEmail(String recipientEmail, String rawToken, String fullName) {
    log.info("[IAM EMAIL] Gửi email đặt lại mật khẩu tới '{}' (Tên: '{}'). Raw Token: '{}'",
        recipientEmail, fullName, rawToken);
  }
}

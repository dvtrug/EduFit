package vn.edufit.ai.application;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edufit.ai.infra.persistence.AiRequestLogEntity;
import vn.edufit.ai.infra.persistence.AiRequestLogRepository;

/** Isolated best-effort audit. Catch outside execute so flush AND commit failures are contained. */
@Service
public class AiRequestLogger {
  private static final Logger log = LoggerFactory.getLogger(AiRequestLogger.class);
  private final AiRequestLogRepository repository;
  private final TransactionTemplate transaction;

  public AiRequestLogger(AiRequestLogRepository repository, PlatformTransactionManager manager) {
    this.repository = repository;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void log(UUID userId, String feature, String status, Integer latencyMs) {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(feature, "feature");
    Objects.requireNonNull(status, "status");
    try {
      transaction.executeWithoutResult(
          tx -> repository.save(new AiRequestLogEntity(userId, feature, status, latencyMs)));
      log.debug("AI audit committed [user: {}, feature: {}, status: {}]", userId, feature, status);
    } catch (RuntimeException ex) {
      // Never retry as ERROR: the provider attempt already has its terminal outcome.
      log.error(
          "AI audit could not be persisted [user: {}, feature: {}, status: {}, type: {}]",
          userId,
          feature,
          status,
          ex.getClass().getSimpleName());
    }
  }
}

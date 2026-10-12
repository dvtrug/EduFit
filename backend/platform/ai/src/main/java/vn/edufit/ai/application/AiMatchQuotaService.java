package vn.edufit.ai.application;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edufit.ai.api.AiMatchQuota;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.infra.config.AiProperties;
import vn.edufit.ai.infra.persistence.AiMatchReservationRepository;

@Service
public class AiMatchQuotaService implements AiMatchQuota {
  private static final Logger log = LoggerFactory.getLogger(AiMatchQuotaService.class);
  private final AiMatchReservationRepository repository;
  private final TransactionTemplate transaction;
  private final Clock clock;
  private final Duration lease;

  public AiMatchQuotaService(
      AiMatchReservationRepository repository,
      PlatformTransactionManager manager,
      @Qualifier("aiQuotaClock") Clock clock,
      AiProperties properties) {
    this.repository = repository;
    this.clock = clock;
    this.lease = properties.timeout().plusSeconds(30);
    this.transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transaction.setTimeout(5);
  }

  @Override
  public Optional<UUID> tryReserve(UUID actor) {
    Objects.requireNonNull(actor, "actor");
    try {
      // Obtain the time inside the short transaction, not before waiting for a pool connection.
      return transaction.execute(
          tx -> {
            var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            return repository.reserve(
                actor, now, now.minus(Duration.ofHours(24)), now.plus(lease), 10);
          });
    } catch (RuntimeException ex) {
      log.warn("AI quota unavailable [actor: {}, type: {}]", actor, ex.getClass().getSimpleName());
      throw new AiUnavailableException("Matching AI quota store unavailable");
    }
  }

  /** Best effort terminal update. A failed update never releases the durable quota attempt. */
  public boolean finish(UUID reservation, String outcome) {
    Objects.requireNonNull(reservation, "reservation");
    if (!Set.of("SUCCESS", "ERROR", "TIMEOUT").contains(outcome)) {
      throw new IllegalArgumentException("Invalid AI attempt outcome");
    }
    try {
      return Boolean.TRUE.equals(
          transaction.execute(
              tx ->
                  repository.finish(
                      reservation, outcome, clock.instant().truncatedTo(ChronoUnit.MICROS))));
    } catch (RuntimeException ex) {
      log.warn(
          "AI reservation outcome could not be persisted [reservation: {}, type: {}]",
          reservation,
          ex.getClass().getSimpleName());
      return false;
    }
  }
}

package vn.edufit.ai.infra.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiMatchReservationRepository {
  private final JdbcTemplate jdbc;

  public AiMatchReservationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Caller must own a short READ_COMMITTED transaction; never call HTTP inside it. */
  public Optional<UUID> reserve(
      UUID actor, Instant now, Instant since, Instant expires, int limit) {
    jdbc.execute("set local lock_timeout = '2s'");
    jdbc.execute("set local statement_timeout = '3s'");
    jdbc.update(
        "insert into ai_match_quota_actor(user_id) values(?) on conflict do nothing", actor);
    jdbc.queryForObject(
        "select user_id from ai_match_quota_actor where user_id=? for update", UUID.class, actor);
    jdbc.update(
        "update ai_match_reservation set status='EXPIRED', finished_at=expires_at where user_id=?"
            + " and status='RESERVED' and expires_at<=?",
        actor,
        Timestamp.from(now));
    Long count =
        jdbc.queryForObject(
            "select count(*) from ai_match_reservation where user_id=? and reserved_at>?",
            Long.class,
            actor,
            Timestamp.from(since));
    if (count >= limit) return Optional.empty();
    UUID id = UUID.randomUUID();
    jdbc.update(
        "insert into ai_match_reservation(reservation_id,user_id,reserved_at,expires_at,status)"
            + " values(?,?,?,?,'RESERVED')",
        id,
        actor,
        Timestamp.from(now),
        Timestamp.from(expires));
    return Optional.of(id);
  }

  public boolean finish(UUID id, String outcome, Instant now) {
    jdbc.execute("set local lock_timeout = '2s'");
    jdbc.execute("set local statement_timeout = '3s'");
    var statuses =
        jdbc.queryForList(
            """
            update ai_match_reservation
            set status=case when expires_at<=? then 'EXPIRED' else ? end,
                finished_at=?
            where reservation_id=? and status='RESERVED'
            returning status
            """,
            String.class,
            Timestamp.from(now),
            outcome,
            Timestamp.from(now),
            id);
    return statuses.size() == 1 && statuses.getFirst().equals(outcome);
  }
}

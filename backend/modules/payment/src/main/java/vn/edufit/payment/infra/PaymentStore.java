package vn.edufit.payment.infra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edufit.payment.application.PaymentViews.*;
import vn.edufit.payment.domain.SessionCharge;
import vn.edufit.shared.exception.InvalidOperationException;

@Repository
public class PaymentStore {
  private final JdbcTemplate db;
  private final TransactionTemplate tx;

  public PaymentStore(
      JdbcTemplate db, org.springframework.transaction.PlatformTransactionManager manager) {
    this.db = db;
    this.tx = new TransactionTemplate(manager);
  }

  // One DB row serializes monetary mutations across application instances. No network calls inside.
  public <T> T locked(Supplier<T> work) {
    return tx.execute(
        s -> {
          db.queryForObject("select id from payment_mutex where id=true for update", Boolean.class);
          return work.get();
        });
  }

  public Wallet wallet(UUID user) {
    return db
        .query(
            "select available,held from payment_wallet where user_id=?",
            (r, n) -> new Wallet(r.getLong(1), r.getLong(2)),
            user)
        .stream()
        .findFirst()
        .orElse(new Wallet(0, 0));
  }

  public void move(UUID user, long available, long held, String key, String kind, UUID ref) {
    if (db.queryForObject("select count(*) from payment_entry where event_key=?", Long.class, key)
        > 0) throw new InvalidOperationException("Giao dịch đã được ghi sổ");
    if (user != null) {
      db.update("insert into payment_wallet(user_id) values (?) on conflict do nothing", user);
      var w = wallet(user);
      if (Math.addExact(w.available(), available) < 0 || Math.addExact(w.held(), held) < 0)
        throw new InvalidOperationException("Số dư khả dụng không đủ");
      db.update(
          "update payment_wallet set available=available+?,held=held+? where user_id=?",
          available,
          held,
          user);
    }
    db.update(
        "insert into payment_entry(user_id,event_key,kind,available_delta,held_delta,reference_id)"
            + " values (?,?,?,?,?,?)",
        user,
        key,
        kind,
        available,
        held,
        ref);
  }

  public void audit(UUID actor, UUID ref, String action, String detail) {
    db.update(
        "insert into payment_audit(actor_id,reference_id,action,detail) values (?,?,?,?)",
        actor,
        ref,
        action,
        detail);
  }

  public List<Map<String, Object>> entries(UUID owner, int offset) {
    return db.queryForList(
        "select id,kind,available_delta,held_delta,reference_id,created_at::text as created_at from"
            + " payment_entry where user_id=? order by created_at desc,id limit 50 offset ?",
        owner,
        offset);
  }

  public Optional<Charge> find(UUID id) {
    return db.query("select * from payment_charge where session_id=?", this::mapCharge, id).stream()
        .findFirst();
  }

  public List<Charge> pending() {
    return db.query(
        "select * from payment_charge where status not in ('SETTLED','REFUNDED','EXPIRED') order by"
            + " updated_at limit 200",
        this::mapCharge);
  }

  public List<Charge> adminCharges(int offset) {
    return db.query(
        "select * from payment_charge where status='AWAITING_ADMIN' order by updated_at limit 50"
            + " offset ?",
        this::mapCharge);
  }

  public void insert(Charge c) {
    db.update(
        "insert into"
            + " payment_charge(session_id,class_id,student_id,tutor_user_id,payer_user_id,amount,commission_bps,end_at,status)"
            + " values (?,?,?,?,?,?,?,?,?)",
        c.sessionId(),
        c.classId(),
        c.studentId(),
        c.tutorUserId(),
        c.payerId(),
        c.state().amount,
        c.commissionBps(),
        Timestamp.from(c.state().endAt),
        c.state().status);
  }

  public void save(Charge c) {
    var s = c.state();
    db.update(
        "update payment_charge set"
            + " end_at=?,status=?,learner_accepted=?,tutor_accepted=?,auto_accepted=?,dispute_reason=?,dispute_opened_at=?,learner_report=?,tutor_report=?,learner_amount=?,tutor_amount=?,settled_amount=?,updated_at=now()"
            + " where session_id=?",
        ts(s.endAt),
        s.status,
        s.learnerAccepted,
        s.tutorAccepted,
        s.autoAccepted,
        s.disputeReason,
        ts(s.disputeOpenedAt),
        s.learnerReport,
        s.tutorReport,
        s.learnerAmount,
        s.tutorAmount,
        s.settledAmount,
        c.sessionId());
  }

  private Charge mapCharge(ResultSet r, int n) throws SQLException {
    var s = new SessionCharge(r.getLong("amount"), r.getTimestamp("end_at").toInstant());
    s.status = r.getString("status");
    s.learnerAccepted = r.getBoolean("learner_accepted");
    s.tutorAccepted = r.getBoolean("tutor_accepted");
    s.autoAccepted = r.getBoolean("auto_accepted");
    s.disputeReason = r.getString("dispute_reason");
    var opened = r.getTimestamp("dispute_opened_at");
    s.disputeOpenedAt = opened == null ? null : opened.toInstant();
    s.learnerReport = r.getString("learner_report");
    s.tutorReport = r.getString("tutor_report");
    s.learnerAmount = (Long) r.getObject("learner_amount");
    s.tutorAmount = (Long) r.getObject("tutor_amount");
    s.settledAmount = r.getLong("settled_amount");
    return new Charge(
        r.getObject("session_id", UUID.class),
        r.getObject("class_id", UUID.class),
        r.getObject("student_id", UUID.class),
        r.getObject("payer_user_id", UUID.class),
        r.getObject("tutor_user_id", UUID.class),
        r.getInt("commission_bps"),
        s);
  }

  public Map<String, Object> createTopup(UUID user, long amount, String provider, Instant expires) {
    return db.queryForMap(
        "insert into payment_topup(id,user_id,amount,provider,status,expires_at) values"
            + " (?,?,?,?,'PENDING',?) returning *",
        UUID.randomUUID(),
        user,
        amount,
        provider,
        ts(expires));
  }

  public void checkout(UUID id, String url, String providerId) {
    db.update(
        "update payment_topup set checkout_url=?,provider_id=? where id=? and status='PENDING'",
        url,
        providerId,
        id);
  }

  public Map<String, Object> topup(UUID id) {
    return one("select * from payment_topup where id=?", id);
  }

  public Optional<Map<String, Object>> findTopupByOrder(long order) {
    return db.queryForList("select * from payment_topup where order_code=?", order).stream()
        .findFirst();
  }

  public List<Map<String, Object>> topups(UUID owner, int offset) {
    return owner == null
        ? db.queryForList(
            "select"
                + " id,user_id,order_code,amount,provider,status,checkout_url,provider_id,transaction_reference,created_at::text"
                + " as created_at,expires_at::text as expires_at from payment_topup order by"
                + " payment_topup.created_at desc,id limit 50 offset ?",
            offset)
        : db.queryForList(
            "select"
                + " id,user_id,order_code,amount,provider,status,checkout_url,provider_id,transaction_reference,created_at::text"
                + " as created_at,expires_at::text as expires_at from payment_topup where user_id=?"
                + " order by payment_topup.created_at desc,id limit 50 offset ?",
            owner,
            offset);
  }

  public void creditTopup(UUID id, String reference, UUID actor) {
    reference = reference.trim();
    var t = topup(id);
    if ("PAID".equals(t.get("status"))) {
      if (!reference.equals(t.get("transaction_reference")))
        throw new InvalidOperationException("Khoản nạp đã có mã giao dịch khác");
      return;
    }
    if (!"PENDING".equals(t.get("status")))
      throw new InvalidOperationException("Khoản nạp không còn hiệu lực");
    UUID owner = (UUID) t.get("user_id");
    long amount = ((Number) t.get("amount")).longValue();
    db.update(
        "update payment_topup set status='PAID',transaction_reference=? where id=?", reference, id);
    move(owner, amount, 0, "topup:" + id, "TOPUP", id);
    audit(actor, id, "TOPUP_CONFIRMED", reference);
  }

  public Map<String, Object> createWithdrawal(
      UUID user, long amount, String bank, String account, String holder) {
    var pending =
        db.queryForList(
            "select * from payment_withdrawal where user_id=? and status='PENDING'", user);
    if (!pending.isEmpty()) {
      var previous = pending.getFirst();
      if (((Number) previous.get("amount")).longValue() == amount
          && bank.equals(previous.get("bank_name"))
          && account.equals(previous.get("bank_account"))
          && holder.equals(previous.get("bank_holder"))) return previous;
      throw new InvalidOperationException("Đã có yêu cầu rút đang chờ xử lý");
    }
    UUID id = UUID.randomUUID();
    move(user, -amount, amount, "withdraw-reserve:" + id, "WITHDRAW_RESERVED", id);
    db.update(
        "insert into"
            + " payment_withdrawal(id,user_id,amount,status,bank_name,bank_account,bank_holder)"
            + " values (?,?,?,'PENDING',?,?,?)",
        id,
        user,
        amount,
        bank,
        account,
        holder);
    return withdrawal(id);
  }

  public Map<String, Object> withdrawal(UUID id) {
    return one("select * from payment_withdrawal where id=?", id);
  }

  public List<Map<String, Object>> withdrawals(UUID owner, int offset) {
    return owner == null
        ? db.queryForList(
            "select"
                + " id,user_id,amount,status,bank_name,bank_account,bank_holder,transaction_reference,admin_note,created_at::text"
                + " as created_at from payment_withdrawal order by payment_withdrawal.created_at"
                + " desc,id limit 50 offset ?",
            offset)
        : db.queryForList(
            "select"
                + " id,user_id,amount,status,bank_name,bank_account,bank_holder,transaction_reference,admin_note,created_at::text"
                + " as created_at from payment_withdrawal where user_id=? order by"
                + " payment_withdrawal.created_at desc,id limit 50 offset ?",
            owner,
            offset);
  }

  public void reviewWithdrawal(UUID id, boolean paid, String reference, String note, UUID actor) {
    reference = reference == null ? null : reference.trim();
    var w = withdrawal(id);
    if (!"PENDING".equals(w.get("status")))
      throw new InvalidOperationException("Yêu cầu rút đã được xử lý");
    long amount = ((Number) w.get("amount")).longValue();
    move(
        (UUID) w.get("user_id"),
        paid ? 0 : amount,
        -amount,
        "withdraw-result:" + id,
        paid ? "WITHDRAW_PAID" : "WITHDRAW_REJECTED",
        id);
    db.update(
        "update payment_withdrawal set status=?,transaction_reference=?,admin_note=? where id=?",
        paid ? "PAID" : "REJECTED",
        paid ? reference : null,
        note,
        id);
    audit(actor, id, paid ? "WITHDRAW_PAID" : "WITHDRAW_REJECTED", note + " | " + reference);
  }

  private Map<String, Object> one(String sql, Object id) {
    var rows = db.queryForList(sql, id);
    if (rows.isEmpty()) throw new InvalidOperationException("Không tìm thấy giao dịch");
    return rows.getFirst();
  }

  private static Timestamp ts(Instant time) {
    return time == null ? null : Timestamp.from(time);
  }
}

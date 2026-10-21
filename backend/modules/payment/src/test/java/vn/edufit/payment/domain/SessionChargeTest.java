package vn.edufit.payment.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SessionChargeTest {
  final Instant end = Instant.parse("2026-10-21T12:00:00Z");

  SessionCharge charge() {
    return new SessionCharge(300_000, end);
  }

  @Test
  void learnerIsAutoAcceptedOnlyAtOneHourAndTutorMustAccept() {
    var c = charge();
    c.tick(end.plusSeconds(3599), true);
    assertFalse(c.learnerAccepted);
    c.tick(end.plusSeconds(3600), true);
    assertTrue(c.learnerAccepted);
    assertFalse(c.terminal());
    c.confirm(true, end.plusSeconds(3601), true);
    assertEquals("SETTLED", c.status);
    assertEquals(300_000, c.settledAmount);
  }

  @Test
  void noChargeBeforeEndOrForUncompletedSession() {
    var c = charge();
    assertThrows(RuntimeException.class, () -> c.confirm(true, end.minusSeconds(1), true));
    c.tick(end.plusSeconds(7200), false);
    assertFalse(c.learnerAccepted);
    assertFalse(c.terminal());
  }

  @Test
  void bothAcceptSettleOnce() {
    var c = charge();
    c.confirm(true, end, true);
    c.confirm(false, end, true);
    assertTrue(c.terminal());
    c.confirm(false, end.plusSeconds(1), true);
    assertEquals(300_000, c.settledAmount);
  }

  @Test
  void disputeStopsAutoSettlementAndExpiresAtExactly24Hours() {
    var c = charge();
    c.confirm(true, end, true);
    c.dispute("Thiếu thời lượng", end);
    c.tick(end.plusSeconds(3600), true);
    assertFalse(c.learnerAccepted);
    c.tick(end.plusSeconds(86400), true);
    assertEquals("EXPIRED", c.status);
    assertEquals(0, c.settledAmount);
    assertThrows(
        RuntimeException.class, () -> c.report(false, 100_000, "Đồng ý", end.plusSeconds(86400)));
  }

  @Test
  void onlyMatchingReportsBeforeDeadlineReachAdmin() {
    var c = charge();
    c.dispute("Vấn đề", end);
    c.report(false, 100_000, "Đề nghị", end);
    c.report(true, 200_000, "Phản hồi", end);
    assertEquals("DISPUTED", c.status);
    c.report(true, 100_000, "Đã thống nhất", end.plusSeconds(86399));
    assertEquals("AWAITING_ADMIN", c.status);
    c.tick(end.plusSeconds(200000), true);
    assertFalse(c.terminal());
    c.approve();
    assertEquals(100_000, c.settledAmount);
  }

  @Test
  void invalidAmountsCannotBeSubmittedOrApproved() {
    var c = charge();
    c.dispute("Vấn đề", end);
    assertThrows(RuntimeException.class, () -> c.report(true, 300_001, "Sai", end));
    assertThrows(RuntimeException.class, c::approve);
  }

  @Test
  void zeroAgreementRefundsEntireReservation() {
    var c = charge();
    c.dispute("Không học", end);
    c.report(false, 0, "Hoàn đủ", end);
    c.report(true, 0, "Đồng ý", end);
    c.approve();
    assertEquals("REFUNDED", c.status);
    assertEquals(0, c.settledAmount);
  }
}

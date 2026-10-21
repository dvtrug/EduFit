package vn.edufit.payment.application;
import java.time.Instant;
import java.util.UUID;
import vn.edufit.payment.domain.SessionCharge;
public final class PaymentViews {
 private PaymentViews() {}
 public record Wallet(long available,long held) {}
 public record Charge(UUID sessionId,UUID classId,UUID studentId,UUID payerId,UUID tutorUserId,int commissionBps,SessionCharge state) {}
 public record Session(UUID sessionId,UUID classId,UUID studentId,String tutorName,String sessionStatus,Instant startAt,Instant endAt,long amount,int commissionBps,UUID payerId,String status,boolean learnerAccepted,boolean tutorAccepted,boolean autoAccepted,Instant disputeDeadline,String disputeReason,Long learnerAmount,Long tutorAmount,String learnerReport,String tutorReport,long settledAmount,boolean canFund,boolean canActAsLearner,boolean canActAsTutor) {}
}

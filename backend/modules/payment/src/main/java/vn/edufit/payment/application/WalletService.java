package vn.edufit.payment.application;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import vn.edufit.payment.infra.*;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.*;

@Service
public class WalletService {
  private final PaymentStore store;
  private final PaymentAccess access;
  private final PaymentSettings settings;
  private final PayosGateway gateway;
  private final PaymentTime clock;

  public WalletService(
      PaymentStore store,
      PaymentAccess access,
      PaymentSettings settings,
      PayosGateway gateway,
      PaymentTime clock) {
    this.store = store;
    this.access = access;
    this.settings = settings;
    this.gateway = gateway;
    this.clock = clock;
  }

  public Map<String, Object> overview(CurrentUser a, int offset) {
    access.authenticated(a);
    UUID user = a.getUserId();
    return Map.of(
        "wallet",
        store.wallet(user),
        "entries",
        store.entries(user, offset),
        "topups",
        store.topups(user, offset),
        "withdrawals",
        store.withdrawals(user, offset),
        "topupEnabled",
        settings.payos() || settings.manual(),
        "topupProvider",
        settings.payos() ? "PAYOS" : "MANUAL");
  }

  public Map<String, Object> topup(CurrentUser a, long amount) {
    access.learnerRole(a);
    money(amount);
    if (!settings.payos() && !settings.manual())
      throw new InvalidOperationException(
          "Chưa cấu hình tài khoản nhận tiền hoặc payOS; liên hệ Admin");
    String provider = settings.payos() ? "PAYOS" : "MANUAL";
    Instant expires = clock.instant().plusSeconds(1800);
    var t = store.locked(() -> store.createTopup(a.getUserId(), amount, provider, expires));
    UUID id = (UUID) t.get("id");
    long order = ((Number) t.get("order_code")).longValue();
    // External HTTP is deliberately outside the database lock/transaction.
    if (settings.payos()) {
      String path = a.hasRole("PARENT") ? "/parent/billing" : "/student/payments";
      var checkout = gateway.create(order, amount, path, expires);
      store.locked(
          () -> {
            store.checkout(id, checkout.url(), checkout.id());
            return null;
          });
      return Map.of("id", id, "checkoutUrl", checkout.url(), "provider", provider);
    }
    String info = "EF" + Long.toString(order, 36).toUpperCase(Locale.ROOT);
    String qr =
        "https://img.vietqr.io/image/"
            + settings.bankBin
            + "-"
            + settings.bankAccount
            + "-compact2.png?amount="
            + amount
            + "&addInfo="
            + enc(info)
            + "&accountName="
            + enc(settings.bankHolder);
    store.locked(
        () -> {
          store.checkout(id, qr, null);
          return null;
        });
    return Map.of(
        "id",
        id,
        "qrUrl",
        qr,
        "bankAccount",
        settings.bankAccount,
        "bankHolder",
        settings.bankHolder,
        "description",
        info,
        "amount",
        amount,
        "provider",
        provider);
  }

  public void webhook(String payload) {
    var receipt = gateway.verify(payload);
    store.locked(
        () -> {
          var found = store.findTopupByOrder(receipt.orderCode());
          // payOS sends a signed sample when registering a webhook. Unknown orders never credit a
          // wallet.
          if (found.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(WalletService.class)
                .warn("Verified payOS webhook has no matching order: {}", receipt.orderCode());
            return null;
          }
          var t = found.get();
          if (!"PAYOS".equals(t.get("provider"))
              || ((Number) t.get("amount")).longValue() != receipt.amount()
              || !"VND".equals(receipt.currency())
              || !receipt.paymentLinkId().equals(t.get("provider_id")))
            throw new InvalidOperationException("Webhook không khớp khoản nạp");
          if (!settings.bankAccount.equals(receipt.account()))
            throw new InvalidOperationException("Sai tài khoản nhận tiền");
          store.creditTopup((UUID) t.get("id"), receipt.reference(), null);
          return null;
        });
  }

  public void confirmManual(CurrentUser a, UUID id, String reference) {
    access.admin(a);
    reference(reference);
    store.locked(
        () -> {
          var t = store.topup(id);
          if (!"MANUAL".equals(t.get("provider")))
            throw new InvalidOperationException("Khoản payOS phải được xác minh bằng webhook");
          store.creditTopup(id, reference, a.getUserId());
          return null;
        });
  }

  public Map<String, Object> withdraw(
      CurrentUser a, long amount, String bank, String account, String holder) {
    access.tutorRole(a);
    money(amount);
    if (bank == null
        || bank.isBlank()
        || bank.length() > 100
        || account == null
        || !account.matches("[0-9]{5,34}")
        || holder == null
        || holder.isBlank()
        || holder.length() > 150)
      throw new InvalidOperationException("Thông tin ngân hàng không hợp lệ");
    return store.locked(
        () -> {
          var w =
              store.createWithdrawal(a.getUserId(), amount, bank.trim(), account, holder.trim());
          store.audit(a.getUserId(), (UUID) w.get("id"), "WITHDRAW_REQUESTED", "VND=" + amount);
          return w;
        });
  }

  public void reviewWithdrawal(
      CurrentUser a, UUID id, boolean paid, String reference, String note) {
    access.admin(a);
    if (paid) reference(reference);
    if (note == null || note.isBlank() || note.length() > 2000)
      throw new InvalidOperationException("Cần ghi chú đối soát");
    store.locked(
        () -> {
          store.reviewWithdrawal(id, paid, reference, note, a.getUserId());
          return null;
        });
  }

  public Map<String, Object> configuration(CurrentUser a) { access.admin(a); return settings.readiness(); }

  public Map<String, Object> admin(CurrentUser a, int offset) {
    access.admin(a);
    return Map.of(
        "topups", store.topups(null, offset), "withdrawals", store.withdrawals(null, offset));
  }

  private static void money(long amount) {
    if (amount < 10000 || amount > 100_000_000)
      throw new InvalidOperationException("Số tiền từ 10.000đ đến 100.000.000đ");
  }

  private static void reference(String value) {
    if (value == null || value.trim().length() < 6 || value.length() > 200)
      throw new InvalidOperationException("Cần mã giao dịch ngân hàng thực tế (6–200 ký tự)");
  }

  private static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}

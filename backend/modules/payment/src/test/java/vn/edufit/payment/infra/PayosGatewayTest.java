package vn.edufit.payment.infra;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;

class PayosGatewayTest {
  PaymentSettings config() {
    var s = new PaymentSettings();
    s.clientId = "client";
    s.apiKey = "api";
    s.checksumKey = "test-checksum";
    s.bankAccount = "12345678";
    return s;
  }

  String payload(Map<String, Object> data) throws Exception {
    return new ObjectMapper()
        .writeValueAsString(
            Map.of(
                "success",
                true,
                "data",
                data,
                "signature",
                PayosGateway.sign(PayosGateway.canonical(new TreeMap<>(data)), "test-checksum")));
  }

  Map<String, Object> data() {
    return new HashMap<>(
        Map.of(
            "orderCode",
            42,
            "amount",
            100000,
            "reference",
            "BANK123",
            "accountNumber",
            "12345678",
            "paymentLinkId",
            "link42",
            "currency",
            "VND",
            "code",
            "00"));
  }

  @Test
  void authenticReceiptHasOrderAmountAndReference() throws Exception {
    var r = new PayosGateway(config()).verify(payload(data()));
    assertEquals(42, r.orderCode());
    assertEquals(100000, r.amount());
    assertEquals("BANK123", r.reference());
  }

  @Test
  void alteredAmountCannotCreditWallet() throws Exception {
    var text = payload(data()).replace("100000", "200000");
    assertThrows(
        vn.edufit.shared.exception.InvalidOperationException.class,
        () -> new PayosGateway(config()).verify(text));
  }

  @Test
  void missingSignatureCannotCreditWallet() {
    assertThrows(
        vn.edufit.shared.exception.InvalidOperationException.class,
        () -> new PayosGateway(config()).verify("{\"success\":true,\"data\":{}}"));
  }

  @Test
  void failedOrFractionalReceiptCannotCreditWallet() throws Exception {
    var d = data();
    d.put("code", "01");
    var failed = payload(d);
    assertThrows(
        vn.edufit.shared.exception.InvalidOperationException.class,
        () -> new PayosGateway(config()).verify(failed));
    d = data();
    d.put("amount", 100000.5);
    var fractional = payload(d);
    assertThrows(
        vn.edufit.shared.exception.InvalidOperationException.class,
        () -> new PayosGateway(config()).verify(fractional));
  }
}

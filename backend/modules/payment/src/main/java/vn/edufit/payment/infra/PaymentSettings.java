package vn.edufit.payment.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentSettings {
  @Value("${PAYOS_CLIENT_ID:}")
  public String clientId = "";

  @Value("${PAYOS_API_KEY:}")
  public String apiKey = "";

  @Value("${PAYOS_CHECKSUM_KEY:}")
  public String checksumKey = "";

  @Value("${PAYMENT_BANK_BIN:}")
  public String bankBin = "";

  @Value("${PAYMENT_BANK_ACCOUNT:}")
  public String bankAccount = "";

  @Value("${PAYMENT_BANK_HOLDER:}")
  public String bankHolder = "";

  @Value("${PAYMENT_FRONTEND_URL:http://localhost:3000}")
  public String frontendUrl = "http://localhost:3000";

  @Value("${PAYOS_PAYOUT_ENABLED:false}")
  public boolean payoutEnabled;
  @Value("${PAYOS_PAYOUT_CLIENT_ID:}")
  public String payoutClientId = "";
  @Value("${PAYOS_PAYOUT_API_KEY:}")
  public String payoutApiKey = "";
  @Value("${PAYOS_PAYOUT_CHECKSUM_KEY:}")
  public String payoutChecksumKey = "";

  public boolean payout() {
    return payoutEnabled && configured(payoutClientId) && configured(payoutApiKey) && configured(payoutChecksumKey);
  }
  private boolean configured(String value) { return value != null && !value.isBlank() && !value.equals("..."); }
  public java.util.Map<String,Object> readiness() {
    return java.util.Map.of("topupReady", payos(), "manualReady", manual(), "payoutReady", payout(),
        "payoutEnabled", payoutEnabled, "webhookPath", "/api/v1/payment/webhooks/payos",
        "frontendUrl", frontendUrl);
  }
  public boolean payos() {
    return configured(clientId)
        && configured(apiKey)
        && configured(checksumKey)
        && bankAccount.matches("[0-9]{5,34}");
  }

  public boolean manual() {
    return bankBin.matches("[0-9]{6}")
        && bankAccount.matches("[0-9]{5,34}")
        && !bankHolder.isBlank();
  }
}

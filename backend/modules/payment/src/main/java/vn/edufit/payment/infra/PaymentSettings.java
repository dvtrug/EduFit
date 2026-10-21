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

  public boolean payos() {
    return !clientId.isBlank()
        && !apiKey.isBlank()
        && !checksumKey.isBlank()
        && bankAccount.matches("[0-9]{5,34}");
  }

  public boolean manual() {
    return bankBin.matches("[0-9]{6}")
        && bankAccount.matches("[0-9]{5,34}")
        && !bankHolder.isBlank();
  }
}

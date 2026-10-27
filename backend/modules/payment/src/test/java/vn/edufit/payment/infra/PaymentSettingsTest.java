package vn.edufit.payment.infra;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PaymentSettingsTest {
 @Test void blankOrPlaceholderKeysNeverEnableLivePayments() {
  var s=new PaymentSettings(); assertFalse(s.payos()); assertFalse(s.payout());
  s.clientId="...";s.apiKey="...";s.checksumKey="...";s.bankAccount="12345678";assertFalse(s.payos());
  s.clientId="client";s.apiKey="api";s.checksumKey="checksum";assertTrue(s.payos());
  s.payoutClientId="payout-client";s.payoutApiKey="payout-api";s.payoutChecksumKey="payout-checksum";
  assertFalse(s.payout());s.payoutEnabled=true;assertTrue(s.payout());
  String status=s.readiness().toString();assertFalse(status.contains("payout-api"));assertFalse(status.contains("checksum"));
 }
}

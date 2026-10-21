package vn.edufit.payment.application;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import vn.edufit.payment.infra.*;
class WalletServiceTest {
 final PaymentStore store=mock(PaymentStore.class);
 final PayosGateway gateway=mock(PayosGateway.class);
 final PaymentSettings settings=new PaymentSettings();
 WalletService service() {
  settings.bankAccount="12345678";
  when(store.locked(any())).thenAnswer(i->((Supplier<?>)i.getArgument(0)).get());
  return new WalletService(store,mock(PaymentAccess.class),settings,gateway,Clock.systemUTC());
 }
 Map<String,Object> topup() {return Map.of("id",UUID.randomUUID(),"provider","PAYOS","amount",100000L,"provider_id","link42");}
 @Test void signedRegistrationProbeNeverCreditsAnyWallet() {
  var service=service();when(gateway.verify("probe")).thenReturn(new PayosGateway.Receipt(123,3000,"PROBE123","12345678","probe","VND"));when(store.findTopupByOrder(123)).thenReturn(Optional.empty());
  service.webhook("probe");verify(store,never()).creditTopup(any(),anyString(),any());
 }
 @Test void wrongAccountOrAmountCannotCreditAWallet() {
  var service=service();when(store.findTopupByOrder(42)).thenReturn(Optional.of(topup()));
  when(gateway.verify("wrong-account")).thenReturn(new PayosGateway.Receipt(42,100000,"BANK123","87654321","link42","VND"));
  when(gateway.verify("wrong-amount")).thenReturn(new PayosGateway.Receipt(42,200000,"BANK123","12345678","link42","VND"));
  assertThrows(RuntimeException.class,()->service.webhook("wrong-account"));assertThrows(RuntimeException.class,()->service.webhook("wrong-amount"));verify(store,never()).creditTopup(any(),anyString(),any());
 }
 @Test void payosCannotBeEnabledWithoutExpectedReceivingAccount() {
  settings.clientId="client";settings.apiKey="api";settings.checksumKey="key";assertFalse(settings.payos());settings.bankAccount="12345678";assertTrue(settings.payos());
 }
}

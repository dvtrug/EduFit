package vn.edufit.payment.infra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PayosTransportTest {
  @SuppressWarnings("unchecked")
  @Test void verifiesProviderSignatureAndNeverAcceptsUnsignedMoneyStatus() throws Exception {
    var http=mock(HttpClient.class);
    HttpResponse<String> response=mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"code\":\"00\",\"data\":{\"amount\":10000,\"status\":\"PAID\"}}");
    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(),(a,b)->true));
    when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var transport=new PayosTransport(http);
    assertThrows(RuntimeException.class,()->transport.call("GET","/v2/payment-requests/42",null,"client","api","secret",false,null));
    when(response.body()).thenReturn("{\"code\":\"00\",\"data\":{\"amount\":10000,\"status\":\"PAID\"},\"signature\":\""+PayosGateway.sign("amount=10000&status=PAID","secret")+"\"}");
    assertEquals("PAID",transport.call("GET","/v2/payment-requests/42",null,"client","api","secret",false,null).path("status").asText());
  }
  @Test void payoutCanonicalMatchesOfficialSdkEncodingAndNestedSorting() {
    assertEquals("amount=10000&description=Hoc%20phi%20%26%20rut&transactions=%5B%7B%22amount%22%3A1%2C%22state%22%3A%22SUCCEEDED%22%7D%5D",
      PayosTransport.canonical(Map.of("description","Hoc phi & rut","amount",10000,"transactions",List.of(Map.of("state","SUCCEEDED","amount",1))),true));
  }
}

package vn.edufit.db;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.edufit.EduFitApplication;
@SpringBootTest(classes=EduFitApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentApplicationIT {
 static PostgreSQLContainer<?> container;
 @DynamicPropertySource static void database(DynamicPropertyRegistry props) {
  String url=System.getenv("PAYMENT_TEST_JDBC");String user=System.getenv().getOrDefault("PAYMENT_TEST_USER","payment_test");String password=System.getenv().getOrDefault("PAYMENT_TEST_PASSWORD","");
  if(url==null) {container=new PostgreSQLContainer<>("postgres:17-alpine");container.start();url=container.getJdbcUrl();user=container.getUsername();password=container.getPassword();}
  String schema="payment_app_"+UUID.randomUUID().toString().replace("-","");String jdbc=url+(url.contains("?")?"&":"?")+"currentSchema="+schema;String u=user,p=password;
  props.add("spring.datasource.url",()->jdbc);props.add("spring.datasource.username",()->u);props.add("spring.datasource.password",()->p);props.add("spring.flyway.schemas",()->schema);props.add("edufit.ai.provider",()->"stub");props.add("spring.mail.test-connection",()->false);
 }
 @Autowired ServletWebServerApplicationContext context;
 @Test void paymentDoesNotBreakApplicationClockWiringOrExposeUnauthenticatedWallet() throws Exception {
  int port=context.getWebServer().getPort();
  try(var http=HttpClient.newHttpClient()) {
   var health=http.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/actuator/health")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,health.statusCode());
   var wallet=http.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/payment/wallet")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(401,wallet.statusCode());
  }
 }
 @AfterAll static void stopContainer(){if(container!=null)container.stop();}
}

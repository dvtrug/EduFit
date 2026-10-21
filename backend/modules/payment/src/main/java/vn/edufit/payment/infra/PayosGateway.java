package vn.edufit.payment.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import vn.edufit.shared.exception.InvalidOperationException;

@Component
public class PayosGateway {
  private final PaymentSettings settings;
  private final ObjectMapper json = new ObjectMapper();

  private static class ClientHolder {
    private static final HttpClient CLIENT =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  public PayosGateway(PaymentSettings settings) {
    this.settings = settings;
  }

  public record Checkout(String url, String id) {}

  public record Receipt(
      long orderCode,
      long amount,
      String reference,
      String account,
      String paymentLinkId,
      String currency) {}

  public Checkout create(long order, long amount, String returnPath, Instant expires) {
    if (!settings.payos()) throw new InvalidOperationException("Chưa cấu hình payOS");
    String base = settings.frontendUrl.replaceAll("/+$", "");
    String callback = base + returnPath;
    String description = "EF" + Long.toString(order, 36).toUpperCase(Locale.ROOT);
    var data = new TreeMap<String, Object>();
    data.put("amount", amount);
    data.put("cancelUrl", callback);
    data.put("description", description);
    data.put("orderCode", order);
    data.put("returnUrl", callback);
    String signature = sign(canonical(data), settings.checksumKey);
    data.put("signature", signature);
    data.put("expiredAt", expires.getEpochSecond());
    try {
      var req =
          HttpRequest.newBuilder(URI.create("https://api-merchant.payos.vn/v2/payment-requests"))
              .timeout(Duration.ofSeconds(20))
              .header("Content-Type", "application/json")
              .header("x-client-id", settings.clientId)
              .header("x-api-key", settings.apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)))
              .build();
      var response = ClientHolder.CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
      var body = json.readTree(response.body());
      if (response.statusCode() != 200 || !"00".equals(body.path("code").asText()))
        throw new InvalidOperationException(
            "Không tạo được QR payOS; hãy thử lại hoặc liên hệ hỗ trợ");
      var result = body.path("data");
      String url = result.path("checkoutUrl").asText();
      String id = result.path("paymentLinkId").asText();
      if (!url.startsWith("https://pay.payos.vn/") || id.isBlank())
        throw new InvalidOperationException("Dữ liệu checkout không hợp lệ");
      return new Checkout(url, id);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidOperationException("Yêu cầu thanh toán bị gián đoạn");
    } catch (java.io.IOException e) {
      throw new InvalidOperationException("Không kết nối được payOS; chưa ghi tăng số dư");
    }
  }

  public Receipt verify(String payload) {
    if (!settings.payos()) throw new InvalidOperationException("Chưa cấu hình payOS");
    if (payload == null || payload.length() > 65536)
      throw new InvalidOperationException("Webhook quá lớn");
    try {
      JsonNode body = json.readTree(payload), data = body.path("data");
      if (!data.isObject()) throw new InvalidOperationException("Webhook không hợp lệ");
      var values = new TreeMap<String, Object>();
      data.fields()
          .forEachRemaining(
              e -> {
                if (e.getValue().isContainerNode())
                  throw new InvalidOperationException("Webhook không hỗ trợ dữ liệu lồng");
                values.put(e.getKey(), e.getValue().isNull() ? "" : e.getValue().asText());
              });
      String received = body.path("signature").asText();
      String expected = sign(canonical(values), settings.checksumKey);
      if (!received.matches("[0-9a-fA-F]{64}")
          || !MessageDigest.isEqual(
              expected.getBytes(StandardCharsets.UTF_8),
              received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)))
        throw new InvalidOperationException("Chữ ký webhook không hợp lệ");
      if (!"00".equals(data.path("code").asText()) || !body.path("success").asBoolean())
        throw new InvalidOperationException("Giao dịch chưa thành công");
      if (!data.path("orderCode").isIntegralNumber()
          || !data.path("amount").isIntegralNumber()
          || !data.path("orderCode").canConvertToLong()
          || !data.path("amount").canConvertToLong())
        throw new InvalidOperationException("Số tiền/mã đơn không hợp lệ");
      String reference = data.path("reference").asText();
      if (reference.isBlank() || reference.length() > 200)
        throw new InvalidOperationException("Thiếu mã giao dịch");
      return new Receipt(
          data.path("orderCode").longValue(),
          data.path("amount").longValue(),
          reference,
          data.path("accountNumber").asText(),
          data.path("paymentLinkId").asText(),
          data.path("currency").asText());
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new InvalidOperationException("Webhook không phải JSON hợp lệ");
    }
  }

  public static String canonical(SortedMap<String, ?> data) {
    return data.entrySet().stream()
        .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
        .collect(java.util.stream.Collectors.joining("&"));
  }

  public static String sign(String value, String key) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Không khởi tạo được HMAC", e);
    }
  }
}

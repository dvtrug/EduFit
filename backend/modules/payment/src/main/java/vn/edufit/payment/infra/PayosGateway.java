package vn.edufit.payment.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import vn.edufit.shared.exception.InvalidOperationException;

@Component
public class PayosGateway {
  private final PaymentSettings settings;
  private final PayosTransport transport;
  private final ObjectMapper json = new ObjectMapper();

  public PayosGateway(PaymentSettings settings) {
    this(settings, new PayosTransport());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public PayosGateway(PaymentSettings settings, PayosTransport transport) {
    this.settings = settings;
    this.transport = transport;
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
    var result =
        transport.call(
            "POST",
            "/v2/payment-requests",
            data,
            settings.clientId,
            settings.apiKey,
            settings.checksumKey,
            false,
            null);
    String url = result.path("checkoutUrl").asText(), id = result.path("paymentLinkId").asText();
    if (!url.startsWith("https://pay.payos.vn/")
        || id.isBlank()
        || !settings.bankAccount.equals(result.path("accountNumber").asText()))
      throw new InvalidOperationException("Dữ liệu checkout/tài khoản nhận không hợp lệ");
    return new Checkout(url, id);
  }

  public JsonNode lookup(long order) {
    if (!settings.payos()) throw new InvalidOperationException("Chưa cấu hình payOS");
    return transport.call(
        "GET",
        "/v2/payment-requests/" + order,
        null,
        settings.clientId,
        settings.apiKey,
        settings.checksumKey,
        false,
        null);
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

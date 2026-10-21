package vn.edufit.payment.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.edufit.payment.application.*;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/payment")
@Validated
public class PaymentController {
  private final WalletService wallets;
  private final SessionPaymentService sessions;
  private final ObjectProvider<CurrentUser> actors;

  public PaymentController(
      WalletService wallets, SessionPaymentService sessions, ObjectProvider<CurrentUser> actors) {
    this.wallets = wallets;
    this.sessions = sessions;
    this.actors = actors;
  }

  public record Money(
      @NotNull @DecimalMin("10000") @DecimalMax("100000000") @Digits(integer = 9, fraction = 0)
          BigDecimal amount) {}

  public record Fund(
      @NotNull @DecimalMin("1") @DecimalMax("100000000") @Digits(integer = 9, fraction = 0)
          BigDecimal amount) {}

  public record Text(@NotBlank @Size(max = 2000) String text) {}

  public record Report(
      @NotNull @DecimalMin("0") @DecimalMax("100000000") @Digits(integer = 9, fraction = 0)
          BigDecimal amount,
      @NotBlank @Size(max = 4000) String text) {}

  public record Withdrawal(
      @NotNull @DecimalMin("10000") @DecimalMax("100000000") @Digits(integer = 9, fraction = 0)
          BigDecimal amount,
      @NotBlank @Size(max = 100) String bank,
      @Pattern(regexp = "[0-9]{5,34}") @NotNull String account,
      @NotBlank @Size(max = 150) String holder) {}

  @GetMapping("/wallet")
  public ApiResponse<?> wallet(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int offset) {
    return ApiResponse.success(wallets.overview(actors.getObject(), offset));
  }

  @PostMapping("/topups")
  public ApiResponse<?> topup(@Valid @RequestBody Money input) {
    return ApiResponse.success(wallets.topup(actors.getObject(), input.amount().longValueExact()));
  }

  @PostMapping("/withdrawals")
  public ApiResponse<?> withdraw(@Valid @RequestBody Withdrawal input) {
    return ApiResponse.success(
        wallets.withdraw(
            actors.getObject(),
            input.amount().longValueExact(),
            input.bank(),
            input.account(),
            input.holder()));
  }

  @GetMapping("/sessions")
  public ApiResponse<?> list(@RequestParam Instant from, @RequestParam Instant to) {
    return ApiResponse.success(sessions.list(actors.getObject(), from, to));
  }

  @GetMapping("/sessions/{id}")
  public ApiResponse<?> session(@PathVariable UUID id) {
    return ApiResponse.success(sessions.get(actors.getObject(), id));
  }

  @PostMapping("/sessions/{id}/fund")
  public ApiResponse<?> fund(@PathVariable UUID id, @Valid @RequestBody Fund input) {
    return ApiResponse.success(
        sessions.fund(actors.getObject(), id, input.amount().longValueExact()));
  }

  @PostMapping("/sessions/{id}/confirm")
  public ApiResponse<?> confirm(@PathVariable UUID id) {
    return ApiResponse.success(sessions.act(actors.getObject(), id, "confirm", null, null));
  }

  @PostMapping("/sessions/{id}/dispute")
  public ApiResponse<?> dispute(@PathVariable UUID id, @Valid @RequestBody Text input) {
    return ApiResponse.success(sessions.act(actors.getObject(), id, "dispute", input.text(), null));
  }

  @PostMapping("/sessions/{id}/report")
  public ApiResponse<?> report(@PathVariable UUID id, @Valid @RequestBody Report input) {
    return ApiResponse.success(
        sessions.act(
            actors.getObject(), id, "report", input.text(), input.amount().longValueExact()));
  }
}

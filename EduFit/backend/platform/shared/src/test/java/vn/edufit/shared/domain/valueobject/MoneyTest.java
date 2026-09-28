package vn.edufit.shared.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kiểm thử Value Object Money")
class MoneyTest {

  @Test
  @DisplayName("Khởi tạo và chuẩn hóa 2 số thập phân thành công")
  void shouldCreateAndNormalizeScaleSuccessfully() {
    Money money = Money.of(150000);

    assertEquals(new BigDecimal("150000.00"), money.amount());
    assertEquals("VND", money.currency());
  }

  @Test
  @DisplayName("Ném ngoại lệ khi số tiền là số âm")
  void shouldThrowExceptionWhenAmountIsNegative() {
    assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.valueOf(-1000), "VND"));
  }

  @Test
  @DisplayName("Thực hiện phép cộng hai khoản tiền cùng đơn vị thành công")
  void shouldAddSuccessfully() {
    Money m1 = Money.of(100000);
    Money m2 = Money.of(50000);

    Money result = m1.add(m2);

    assertEquals(new BigDecimal("150000.00"), result.amount());
    assertEquals("VND", result.currency());
  }

  @Test
  @DisplayName("Thực hiện phép trừ thành công")
  void shouldSubtractSuccessfully() {
    Money m1 = Money.of(200000);
    Money m2 = Money.of(70000);

    Money result = m1.subtract(m2);

    assertEquals(new BigDecimal("130000.00"), result.amount());
  }

  @Test
  @DisplayName("Ném ngoại lệ khi phép trừ cho kết quả âm")
  void shouldThrowExceptionWhenSubtractionResultsInNegative() {
    Money m1 = Money.of(50000);
    Money m2 = Money.of(100000);

    assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
  }

  @Test
  @DisplayName("Ném ngoại lệ khi thực hiện phép toán trên hai loại tiền tệ khác nhau")
  void shouldThrowExceptionWhenCurrenciesDoNotMatch() {
    Money vnd = Money.of(100000);
    Money usd = new Money(BigDecimal.valueOf(10), "USD");

    assertThrows(IllegalArgumentException.class, () -> vnd.add(usd));
    assertThrows(IllegalArgumentException.class, () -> vnd.subtract(usd));
  }

  @Test
  @DisplayName("Thực hiện phép nhân với hệ số thành công")
  void shouldMultiplySuccessfully() {
    Money hourlyRate = Money.of(200000);
    Money total = hourlyRate.multiply(3); // 3 buổi học

    assertEquals(new BigDecimal("600000.00"), total.amount());
  }

  @Test
  @DisplayName("So sánh lớn hơn chính xác")
  void shouldCompareGreaterThanCorrectly() {
    Money high = Money.of(300000);
    Money low = Money.of(150000);

    assertTrue(high.isGreaterThan(low));
    assertFalse(low.isGreaterThan(high));
  }
}

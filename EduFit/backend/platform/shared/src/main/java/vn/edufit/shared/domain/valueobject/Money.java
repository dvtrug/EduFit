package vn.edufit.shared.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object (Đối tượng giá trị) đại diện cho tiền tệ và số tiền trong hệ thống EduFit.
 *
 * <p>Mục đích thiết kế:
 * <ul>
 *   <li><b>Tính bất biến (Immutability):</b> Dùng {@code record}, giá trị không thể bị thay đổi sau khi tạo.
 *       Mọi phép tính toán (cộng, trừ, nhân) đều sinh ra một đối tượng {@code Money} mới.</li>
 *   <li><b>Chống sai số làm tròn số thực:</b> Sử dụng {@link BigDecimal} thay vì kiểu {@code double} hoặc {@code float}
 *       để tránh lỗi sai số dấu phẩy động nhị phân của máy tính trong giao dịch tài chính.</li>
 *   <li><b>Tự kiểm tra hợp lệ (Self-Validation):</b> Ngăn chặn ngay lập tức việc số tiền bị âm hoặc đơn vị tiền tệ bị rỗng.</li>
 *   <li><b>Hỗ trợ đa module:</b> Dùng chung cho học phí gia sư (Profile), thỏa thuận hợp đồng (Connection),
 *       bộ lọc khoảng giá (Discovery) và tổng chi phí học (Progress).</li>
 * </ul>
 *
 * @param amount   Số tiền, được chuẩn hóa với 2 chữ số thập phân (scale = 2).
 * @param currency Mã đơn vị tiền tệ (mặc định là "VND").
 */
public record Money(BigDecimal amount, String currency) {

  /** Đơn vị tiền tệ mặc định của toàn hệ thống là Việt Nam Đồng (VND). */
  public static final String DEFAULT_CURRENCY = "VND";

  /** Hằng số đại diện cho số tiền 0 VND. */
  public static final Money ZERO = new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);

  /**
   * Compact Constructor thực hiện kiểm tra tính hợp lệ và chuẩn hóa số tiền ngay khi khởi tạo.
   *
   * @throws NullPointerException     Nếu amount hoặc currency là null.
   * @throws IllegalArgumentException Nếu amount có giá trị âm hoặc currency bị rỗng.
   */
  public Money {
    Objects.requireNonNull(amount, "Số tiền (amount) không được phép null");
    Objects.requireNonNull(currency, "Đơn vị tiền tệ (currency) không được phép null");

    if (currency.isBlank()) {
      throw new IllegalArgumentException("Đơn vị tiền tệ không được để trống");
    }

    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Số tiền không được phép là số âm: " + amount);
    }

    // Chuẩn hóa số tiền: giữ 2 chữ số thập phân, làm tròn nửa lên (HALF_UP)
    amount = amount.setScale(2, RoundingMode.HALF_UP);
    currency = currency.trim().toUpperCase();
  }

  /**
   * Phương thức tiện ích (Factory method) tạo đối tượng Money từ số nguyên long với đơn vị mặc định VND.
   *
   * @param amountValue Số tiền kiểu long (ví dụ: 150000 -> 150.000 VND).
   * @return Đối tượng Money hợp lệ.
   */
  public static Money of(long amountValue) {
    return new Money(BigDecimal.valueOf(amountValue), DEFAULT_CURRENCY);
  }

  /**
   * Phương thức tiện ích tạo đối tượng Money từ BigDecimal với đơn vị mặc định VND.
   *
   * @param amountValue Số tiền kiểu BigDecimal.
   * @return Đối tượng Money hợp lệ.
   */
  public static Money of(BigDecimal amountValue) {
    return new Money(amountValue, DEFAULT_CURRENCY);
  }

  /**
   * Thực hiện phép cộng hai khoản tiền.
   *
   * @param other Khoản tiền cần cộng thêm.
   * @return Đối tượng Money mới mang kết quả của phép cộng.
   * @throws IllegalArgumentException Nếu hai đối tượng khác đơn vị tiền tệ.
   */
  public Money add(Money other) {
    Objects.requireNonNull(other, "Đối tượng Money cần cộng không được phép null");
    assertSameCurrency(other);
    return new Money(this.amount.add(other.amount), this.currency);
  }

  /**
   * Thực hiện phép trừ khoản tiền hiện tại cho một khoản tiền khác.
   *
   * @param other Khoản tiền cần trừ bớt.
   * @return Đối tượng Money mới mang kết quả của phép trừ.
   * @throws IllegalArgumentException Nếu hai đối tượng khác đơn vị tiền tệ hoặc kết quả ra số âm.
   */
  public Money subtract(Money other) {
    Objects.requireNonNull(other, "Đối tượng Money cần trừ không được phép null");
    assertSameCurrency(other);
    BigDecimal resultAmount = this.amount.subtract(other.amount);
    if (resultAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Số tiền sau khi trừ không được phép âm: " + resultAmount);
    }
    return new Money(resultAmount, this.currency);
  }

  /**
   * Nhân số tiền với một hệ số (ví dụ: nhân số giờ học để tính tổng học phí).
   *
   * @param factor Hệ số nhân kiểu int hoặc long.
   * @return Đối tượng Money mới mang kết quả nhân.
   */
  public Money multiply(long factor) {
    if (factor < 0) {
      throw new IllegalArgumentException("Hệ số nhân không được là số âm: " + factor);
    }
    return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
  }

  /**
   * So sánh xem khoản tiền hiện tại có lớn hơn khoản tiền khác không.
   *
   * @param other Khoản tiền đem ra so sánh.
   * @return {@code true} nếu khoản tiền này lớn hơn {@code other}.
   */
  public boolean isGreaterThan(Money other) {
    Objects.requireNonNull(other, "Đối tượng so sánh không được null");
    assertSameCurrency(other);
    return this.amount.compareTo(other.amount) > 0;
  }

  /**
   * Đảm bảo hai khoản tiền có cùng đơn vị trước khi thực hiện các phép tính số học.
   */
  private void assertSameCurrency(Money other) {
    if (!this.currency.equalsIgnoreCase(other.currency)) {
      throw new IllegalArgumentException(
          "Không thể thực hiện phép toán trên hai loại tiền tệ khác nhau: " + this.currency + " và " + other.currency
      );
    }
  }
}

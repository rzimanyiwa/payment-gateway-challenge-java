package com.checkout.payment.gateway.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.YearMonth;

public record PostPaymentRequest(
    @NotNull @Pattern(regexp = "\\d{14,19}", message = "must be 14-19 digits")
    String cardNumber,
    @NotNull @Min(1) @Max(12)
    Integer expiryMonth,
    @NotNull
    Integer expiryYear,
    @NotNull @Pattern(regexp = "USD|GBP|EUR", message = "must be one of USD, GBP, EUR")
    String currency,
    @NotNull @Positive
    Integer amount,
    @NotNull @Pattern(regexp = "\\d{3,4}", message = "must be 3-4 digits")
    String cvv) {

  @AssertTrue(message = "card has expired")
  public boolean isExpiryDateInFuture() {
    if (expiryMonth == null || expiryYear == null || expiryMonth < 1 || expiryMonth > 12) {
      return true;
    }
    YearMonth now = YearMonth.now();
    return expiryYear > now.getYear()
        || (expiryYear == now.getYear() && expiryMonth >= now.getMonthValue());
  }

  @Override
  public String toString() {
    return "PostPaymentRequest[expiryMonth=" + expiryMonth + ", expiryYear=" + expiryYear
        + ", currency=" + currency + ", amount=" + amount + "]";
  }
}

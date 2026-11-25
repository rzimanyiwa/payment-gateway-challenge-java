package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record BankPaymentRequest(
    String cardNumber,
    String expiryDate,
    String currency,
    int amount,
    String cvv) {

  public static BankPaymentRequest from(PostPaymentRequest request) {
    return new BankPaymentRequest(
        request.cardNumber(),
        String.format("%02d/%d", request.expiryMonth(), request.expiryYear()),
        request.currency(),
        request.amount(),
        request.cvv());
  }

  @Override
  public String toString() {
    return "BankPaymentRequest[expiryDate=" + expiryDate + ", currency=" + currency
        + ", amount=" + amount + "]";
  }
}

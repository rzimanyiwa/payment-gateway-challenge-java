package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    @Schema(implementation = String.class, allowableValues = {"Authorized", "Declined"})
    PaymentStatus status,
    String cardNumberLastFour,
    int expiryMonth,
    int expiryYear,
    String currency,
    int amount) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(payment.id(), payment.status(), payment.cardNumberLastFour(),
        payment.expiryMonth(), payment.expiryYear(), payment.currency(), payment.amount());
  }
}
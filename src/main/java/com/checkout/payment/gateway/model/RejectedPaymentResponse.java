package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RejectedPaymentResponse(
    @Schema(implementation = String.class, allowableValues = "Rejected") PaymentStatus status,
    List<FieldError> errors) {

  public RejectedPaymentResponse(List<FieldError> errors) {
    this(PaymentStatus.REJECTED, errors);
  }

  public record FieldError(String field, String message) {
  }
}

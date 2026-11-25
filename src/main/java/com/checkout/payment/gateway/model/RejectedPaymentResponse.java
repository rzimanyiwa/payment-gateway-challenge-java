package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.List;

public record RejectedPaymentResponse(PaymentStatus status, List<FieldError> errors) {

  public RejectedPaymentResponse(List<FieldError> errors) {
    this(PaymentStatus.REJECTED, errors);
  }

  public record FieldError(String field, String message) {
  }
}

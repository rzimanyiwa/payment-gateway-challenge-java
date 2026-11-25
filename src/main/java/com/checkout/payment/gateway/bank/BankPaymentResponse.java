package com.checkout.payment.gateway.bank;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record BankPaymentResponse(boolean authorized, String authorizationCode) {
}

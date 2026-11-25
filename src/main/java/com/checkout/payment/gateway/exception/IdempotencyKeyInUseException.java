package com.checkout.payment.gateway.exception;

public class IdempotencyKeyInUseException extends RuntimeException {

  public IdempotencyKeyInUseException(String key) {
    super("Idempotency key " + key + " is still in progress");
  }
}

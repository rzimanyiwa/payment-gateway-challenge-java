package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.RejectedPaymentResponse;
import com.checkout.payment.gateway.model.RejectedPaymentResponse.FieldError;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);
  private static final SnakeCaseStrategy SNAKE_CASE = new SnakeCaseStrategy();

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("Payment not found"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RejectedPaymentResponse> handleInvalidRequest(
      MethodArgumentNotValidException ex) {
    List<FieldError> errors = ex.getFieldErrors().stream()
        .map(e -> new FieldError(SNAKE_CASE.translate(e.getField()), e.getDefaultMessage()))
        .toList();
    LOG.info("Payment rejected: {}", errors);
    return ResponseEntity.badRequest().body(new RejectedPaymentResponse(errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RejectedPaymentResponse> handleUnreadableRequest(
      HttpMessageNotReadableException ex) {
    LOG.info("Payment rejected: unreadable request body");
    return ResponseEntity.badRequest().body(new RejectedPaymentResponse(
        List.of(new FieldError("body", "malformed JSON or a field has the wrong type"))));
  }
}

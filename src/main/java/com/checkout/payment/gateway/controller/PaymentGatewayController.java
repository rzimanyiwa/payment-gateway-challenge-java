package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.RejectedPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping
  @ApiResponse(responseCode = "201", description = "Authorized or Declined",
      content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
  @ApiResponse(responseCode = "400", description = "Rejected: invalid request",
      content = @Content(schema = @Schema(implementation = RejectedPaymentResponse.class)))
  @ApiResponse(responseCode = "409", description = "Idempotency-Key still in progress",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "502", description = "Acquiring bank unavailable",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PostPaymentRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
    PaymentResponse response =
        PaymentResponse.from(paymentGatewayService.processPayment(request, idempotencyKey));
    return ResponseEntity.created(URI.create("/v1/payments/" + response.id())).body(response);
  }

  @GetMapping("/{id}")
  @ApiResponse(responseCode = "200", description = "The payment",
      content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
  @ApiResponse(responseCode = "400", description = "id is not a valid UUID", content = @Content)
  @ApiResponse(responseCode = "404", description = "Payment not found",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
    return ResponseEntity.ok(PaymentResponse.from(paymentGatewayService.getPaymentById(id)));
  }
}

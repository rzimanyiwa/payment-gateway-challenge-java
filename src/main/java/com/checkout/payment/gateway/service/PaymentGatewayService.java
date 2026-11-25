package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankPaymentRequest;
import com.checkout.payment.gateway.bank.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public Payment getPaymentById(UUID id) {
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public Payment processPayment(PostPaymentRequest request) {
    BankPaymentResponse bankResponse = bankClient.authorize(BankPaymentRequest.from(request));
    PaymentStatus status =
        bankResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
    String cardNumber = request.cardNumber();

    Payment payment = new Payment(
        UUID.randomUUID(),
        status,
        cardNumber.substring(cardNumber.length() - 4),
        request.expiryMonth(),
        request.expiryYear(),
        request.currency(),
        request.amount(),
        bankResponse.authorizationCode());
    paymentsRepository.add(payment);

    LOG.info("Payment {} {}: {} {}", payment.id(), status, payment.amount(), payment.currency());
    return payment;
  }
}

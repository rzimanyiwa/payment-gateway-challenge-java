package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankPaymentResponse;
import com.checkout.payment.gateway.bank.BankUnavailableException;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.IdempotencyKeyInUseException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.IdempotencyKeys;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentGatewayServiceTest {

  private static final PostPaymentRequest REQUEST =
      new PostPaymentRequest("2222405343248877", 4, 2030, "GBP", 100, "123");

  private final BankClient bankClient = mock(BankClient.class);
  private final PaymentsRepository paymentsRepository = mock(PaymentsRepository.class);
  private final IdempotencyKeys idempotencyKeys = new IdempotencyKeys();
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final PaymentGatewayService service = new PaymentGatewayService(paymentsRepository,
      idempotencyKeys, bankClient, meterRegistry);

  @Test
  void whenBankAuthorizesThenPaymentIsStoredAsAuthorized() {
    when(bankClient.authorize(any())).thenReturn(new BankPaymentResponse(true, "auth-123"));

    Payment payment = service.processPayment(REQUEST, null);

    assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(payment.cardNumberLastFour()).isEqualTo("8877");
    assertThat(payment.authorizationCode()).isEqualTo("auth-123");
    verify(paymentsRepository).add(payment);
    assertThat(meterRegistry.counter("payments", "status", "Authorized").count()).isEqualTo(1);
  }

  @Test
  void whenBankDeclinesThenPaymentIsStoredAsDeclined() {
    when(bankClient.authorize(any())).thenReturn(new BankPaymentResponse(false, ""));

    Payment payment = service.processPayment(REQUEST, null);

    assertThat(payment.status()).isEqualTo(PaymentStatus.DECLINED);
    verify(paymentsRepository).add(payment);
    assertThat(meterRegistry.counter("payments", "status", "Declined").count()).isEqualTo(1);
  }

  @Test
  void whenBankIsUnavailableThenNothingIsStored() {
    when(bankClient.authorize(any()))
        .thenThrow(new BankUnavailableException("Bank call failed", null));

    assertThatThrownBy(() -> service.processPayment(REQUEST, null))
        .isInstanceOf(BankUnavailableException.class);
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenIdempotencyKeyIsRepeatedThenBankIsCalledOnce() {
    when(bankClient.authorize(any())).thenReturn(new BankPaymentResponse(true, "auth-123"));
    Payment first = service.processPayment(REQUEST, "key-1");
    when(paymentsRepository.get(first.id())).thenReturn(Optional.of(first));

    Payment second = service.processPayment(REQUEST, "key-1");

    assertThat(second).isEqualTo(first);
    verify(bankClient, times(1)).authorize(any());
  }

  @Test
  void whenIdempotencyKeyIsStillInProgressThenItIsRefused() {
    idempotencyKeys.reserve("key-1");

    assertThatThrownBy(() -> service.processPayment(REQUEST, "key-1"))
        .isInstanceOf(IdempotencyKeyInUseException.class);
    verify(bankClient, never()).authorize(any());
  }

  @Test
  void whenBankFailsThenTheSameIdempotencyKeyCanBeRetried() {
    when(bankClient.authorize(any()))
        .thenThrow(new BankUnavailableException("Bank call failed", null))
        .thenReturn(new BankPaymentResponse(true, "auth-123"));

    assertThatThrownBy(() -> service.processPayment(REQUEST, "key-1"))
        .isInstanceOf(BankUnavailableException.class);
    Payment payment = service.processPayment(REQUEST, "key-1");

    assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    verify(bankClient, times(2)).authorize(any());
  }
}

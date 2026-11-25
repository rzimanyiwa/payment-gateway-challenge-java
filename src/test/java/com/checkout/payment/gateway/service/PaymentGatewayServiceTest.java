package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankPaymentResponse;
import com.checkout.payment.gateway.bank.BankUnavailableException;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;

class PaymentGatewayServiceTest {

  private static final PostPaymentRequest REQUEST =
      new PostPaymentRequest("2222405343248877", 4, 2030, "GBP", 100, "123");

  private final BankClient bankClient = mock(BankClient.class);
  private final PaymentsRepository paymentsRepository = mock(PaymentsRepository.class);
  private final PaymentGatewayService service =
      new PaymentGatewayService(paymentsRepository, bankClient);

  @Test
  void whenBankAuthorizesThenPaymentIsStoredAsAuthorized() {
    when(bankClient.authorize(any())).thenReturn(new BankPaymentResponse(true, "auth-123"));

    Payment payment = service.processPayment(REQUEST);

    assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(payment.cardNumberLastFour()).isEqualTo("8877");
    assertThat(payment.authorizationCode()).isEqualTo("auth-123");
    verify(paymentsRepository).add(payment);
  }

  @Test
  void whenBankDeclinesThenPaymentIsStoredAsDeclined() {
    when(bankClient.authorize(any())).thenReturn(new BankPaymentResponse(false, ""));

    Payment payment = service.processPayment(REQUEST);

    assertThat(payment.status()).isEqualTo(PaymentStatus.DECLINED);
    verify(paymentsRepository).add(payment);
  }

  @Test
  void whenBankIsUnavailableThenNothingIsStored() {
    when(bankClient.authorize(any()))
        .thenThrow(new BankUnavailableException("Bank call failed", null));

    assertThatThrownBy(() -> service.processPayment(REQUEST))
        .isInstanceOf(BankUnavailableException.class);
    verify(paymentsRepository, never()).add(any());
  }
}

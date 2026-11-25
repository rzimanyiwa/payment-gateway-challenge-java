package com.checkout.payment.gateway.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class BankClientTest {

  private static final String BANK_URL = "http://bank/payments";
  private static final PostPaymentRequest REQUEST =
      new PostPaymentRequest("2222405343248877", 4, 2030, "GBP", 100, "123");

  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer bank = MockRestServiceServer.bindTo(restTemplate).build();
  private final BankClient bankClient = new BankClient(restTemplate, BANK_URL);

  @Test
  void whenBankAuthorizesThenPaymentIsAuthorized() {
    bank.expect(requestTo(BANK_URL))
        .andExpect(method(POST))
        .andExpect(content().json("""
            {"card_number": "2222405343248877", "expiry_date": "04/2030",
             "currency": "GBP", "amount": 100, "cvv": "123"}"""))
        .andRespond(withSuccess("""
            {"authorized": true, "authorization_code": "auth-123"}""", APPLICATION_JSON));

    BankPaymentResponse response = bankClient.authorize(BankPaymentRequest.from(REQUEST));

    assertThat(response.authorized()).isTrue();
    assertThat(response.authorizationCode()).isEqualTo("auth-123");
  }

  @Test
  void whenBankDeclinesThenPaymentIsNotAuthorized() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(withSuccess("""
            {"authorized": false, "authorization_code": ""}""", APPLICATION_JSON));

    assertThat(bankClient.authorize(BankPaymentRequest.from(REQUEST)).authorized()).isFalse();
  }

  @Test
  void whenBankIsUnavailableThenBankUnavailableExceptionIsThrown() {
    bank.expect(requestTo(BANK_URL)).andRespond(withServiceUnavailable());

    assertThatThrownBy(() -> bankClient.authorize(BankPaymentRequest.from(REQUEST)))
        .isInstanceOf(BankUnavailableException.class);
  }
}

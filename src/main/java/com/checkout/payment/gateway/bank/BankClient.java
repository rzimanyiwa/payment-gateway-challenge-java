package com.checkout.payment.gateway.bank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public BankClient(RestTemplate restTemplate, @Value("${bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  public BankPaymentResponse authorize(BankPaymentRequest request) {
    BankPaymentResponse response;
    try {
      response = restTemplate.postForObject(bankUrl, request, BankPaymentResponse.class);
    } catch (RestClientException e) {
      throw new BankUnavailableException("Bank call failed", e);
    }
    if (response == null) {
      throw new BankUnavailableException("Bank returned an empty response", null);
    }
    return response;
  }
}

package com.checkout.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag("e2e")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PaymentGatewayEndToEndTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void whenCardNumberEndsInOddDigitThenPaymentIsAuthorizedAndCanBeFetched() {
    ResponseEntity<JsonNode> created = pay("2222405343248877");

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().get("status").asText()).isEqualTo("Authorized");

    ResponseEntity<JsonNode> fetched =
        restTemplate.getForEntity(created.getHeaders().getLocation(), JsonNode.class);
    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isEqualTo(created.getBody());
  }

  @Test
  void whenCardNumberEndsInEvenDigitThenPaymentIsDeclined() {
    ResponseEntity<JsonNode> created = pay("2222405343248876");

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().get("status").asText()).isEqualTo("Declined");
  }

  @Test
  void whenCardNumberEndsInZeroThenBankIsUnavailable() {
    assertThat(pay("2222405343248870").getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
  }

  private ResponseEntity<JsonNode> pay(String cardNumber) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String body = """
        {"card_number": "%s", "expiry_month": 4, "expiry_year": 2030,
         "currency": "GBP", "amount": 100, "cvv": "123"}""".formatted(cardNumber);
    return restTemplate.postForEntity("/v1/payments", new HttpEntity<>(body, headers),
        JsonNode.class);
  }
}

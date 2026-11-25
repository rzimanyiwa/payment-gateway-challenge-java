package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankPaymentResponse;
import com.checkout.payment.gateway.bank.BankUnavailableException;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  private static final String VALID_REQUEST = """
      {"card_number": "2222405343248877", "expiry_month": 4, "expiry_year": 2030,
       "currency": "GBP", "amount": 100, "cvv": "123"}""";

  @Autowired
  private MockMvc mvc;
  @Autowired
  private PaymentsRepository paymentsRepository;
  @MockBean
  private BankClient bankClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    Payment payment = new Payment(UUID.randomUUID(), PaymentStatus.AUTHORIZED, "0123", 12, 2030,
        "USD", 1050, "auth-code");
    paymentsRepository.add(payment);

    mvc.perform(get("/v1/payments/" + payment.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(payment.id().toString()))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("0123"))
        .andExpect(jsonPath("$.expiry_month").value(12))
        .andExpect(jsonPath("$.expiry_year").value(2030))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(1050))
        .andExpect(jsonPath("$.authorization_code").doesNotExist());
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(get("/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  @Test
  void whenPaymentRequestIsInvalidThenItIsRejected() throws Exception {
    mvc.perform(post("/v1/payments").contentType(APPLICATION_JSON).content("""
            {"card_number": "1234", "expiry_month": 13, "expiry_year": 2030,
             "currency": "GBP", "amount": 100, "cvv": "123"}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.errors[*].field",
            containsInAnyOrder("card_number", "expiry_month")));
    verifyNoInteractions(bankClient);
  }

  @Test
  void whenAmountIsNotAWholeNumberThenItIsRejected() throws Exception {
    mvc.perform(post("/v1/payments").contentType(APPLICATION_JSON).content("""
            {"card_number": "2222405343248877", "expiry_month": 4, "expiry_year": 2030,
             "currency": "GBP", "amount": 10.5, "cvv": "123"}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenBankAuthorizesThenPaymentIsCreated() throws Exception {
    when(bankClient.authorize(any())).thenReturn(new BankPaymentResponse(true, "auth-123"));

    mvc.perform(post("/v1/payments").contentType(APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", startsWith("/v1/payments/")))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.card_number").doesNotExist())
        .andExpect(jsonPath("$.authorization_code").doesNotExist());
  }

  @Test
  void whenBankIsUnavailableThen502IsReturned() throws Exception {
    when(bankClient.authorize(any()))
        .thenThrow(new BankUnavailableException("Bank call failed", null));

    mvc.perform(post("/v1/payments").contentType(APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Acquiring bank unavailable, please retry"));
  }
}

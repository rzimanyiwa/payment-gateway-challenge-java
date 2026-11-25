package com.checkout.payment.gateway.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PostPaymentRequestTest {

  private static final String CARD = "2222405343248877";
  private static final YearMonth NEXT_YEAR = YearMonth.now().plusYears(1);
  private static final int MONTH = NEXT_YEAR.getMonthValue();
  private static final int YEAR = NEXT_YEAR.getYear();
  private static final String CURRENCY = "GBP";
  private static final int AMOUNT = 1050;
  private static final String CVV = "012";

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void whenRequestIsValidThenThereAreNoErrors() {
    assertThat(invalidFields(request(CARD, MONTH, YEAR, CURRENCY, AMOUNT, CVV))).isEmpty();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"1234567890123", "12345678901234567890", "2222405343248a77"})
  void whenCardNumberIsInvalidThenItIsRejected(String cardNumber) {
    assertThat(invalidFields(request(cardNumber, MONTH, YEAR, CURRENCY, AMOUNT, CVV)))
        .containsExactly("cardNumber");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {0, 13})
  void whenExpiryMonthIsInvalidThenItIsRejected(Integer month) {
    assertThat(invalidFields(request(CARD, month, YEAR, CURRENCY, AMOUNT, CVV)))
        .containsExactly("expiryMonth");
  }

  @Test
  void whenCardExpiredLastMonthThenItIsRejected() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    PostPaymentRequest request = request(CARD, lastMonth.getMonthValue(), lastMonth.getYear(),
        CURRENCY, AMOUNT, CVV);

    assertThat(invalidFields(request)).containsExactly("expiryDateInFuture");
  }

  @Test
  void whenCardExpiresThisMonthThenItIsAccepted() {
    YearMonth thisMonth = YearMonth.now();
    PostPaymentRequest request = request(CARD, thisMonth.getMonthValue(), thisMonth.getYear(),
        CURRENCY, AMOUNT, CVV);

    assertThat(invalidFields(request)).isEmpty();
  }

  @Test
  void whenExpiryYearIsVeryLargeThenItIsAccepted() {
    assertThat(invalidFields(request(CARD, MONTH, Integer.MAX_VALUE, CURRENCY, AMOUNT, CVV)))
        .isEmpty();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"JPY", "usd"})
  void whenCurrencyIsNotSupportedThenItIsRejected(String currency) {
    assertThat(invalidFields(request(CARD, MONTH, YEAR, currency, AMOUNT, CVV)))
        .containsExactly("currency");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {0, -1})
  void whenAmountIsNotPositiveThenItIsRejected(Integer amount) {
    assertThat(invalidFields(request(CARD, MONTH, YEAR, CURRENCY, amount, CVV)))
        .containsExactly("amount");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"12", "12345", "12a"})
  void whenCvvIsInvalidThenItIsRejected(String cvv) {
    assertThat(invalidFields(request(CARD, MONTH, YEAR, CURRENCY, AMOUNT, cvv)))
        .containsExactly("cvv");
  }

  @Test
  void whenRequestIsPrintedThenCardNumberAndCvvAreHidden() {
    String printed = request(CARD, MONTH, YEAR, CURRENCY, AMOUNT, "987").toString();

    assertThat(printed).doesNotContain(CARD).doesNotContain("987");
  }

  private Set<String> invalidFields(PostPaymentRequest request) {
    return validator.validate(request).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  private static PostPaymentRequest request(String cardNumber, Integer expiryMonth,
      Integer expiryYear, String currency, Integer amount, String cvv) {
    return new PostPaymentRequest(cardNumber, expiryMonth, expiryYear, currency, amount, cvv);
  }
}

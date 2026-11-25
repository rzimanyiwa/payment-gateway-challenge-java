# Payment Gateway

A payment gateway for the Checkout.com take-home challenge
([requirements](https://github.com/cko-recruitment/)). Merchants send a card payment; the gateway
validates it, asks the acquiring bank (a simulator) to authorize it, and stores the result
(`Authorized` or `Declined`) for the merchant to fetch later. Invalid requests are `Rejected`
without calling the bank.

## Running it

```bash
docker compose up
```

This builds the gateway (rebuilt on every `up`, so it always runs the current code) and starts
it with the bank simulator.

- API: `http://localhost:8090/v1/payments`
- Swagger UI (full schemas and field rules): `http://localhost:8090/swagger-ui/index.html`
- Health: `http://localhost:8090/actuator/health`

To run the gateway from the IDE instead (JDK 17): `docker compose up -d bank_simulator`, then
`./gradlew bootRun`.

## Testing

```bash
./gradlew test       # unit and integration tests, bank mocked, no Docker needed
./gradlew e2eTest    # end-to-end tests against the real simulator (start it first)
```

## API

JSON is snake_case. Amounts are integers in the currency's **minor unit** (`1050` = 10.50 GBP).

```bash
curl -i -X POST localhost:8090/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-42' \
  -d '{"card_number": "2222405343248877", "expiry_month": 4, "expiry_year": 2030,
       "currency": "GBP", "amount": 1050, "cvv": "123"}'
```

| Status | Meaning | What the merchant should do |
|---|---|---|
| `201` + `Location` | Authorized or Declined; payment stored | Fetch it later from `Location` |
| `400` | Rejected: `errors` lists every invalid field; the bank was not called | Fix the request |
| `409` | A request with this `Idempotency-Key` is still in progress | Retry shortly |
| `502` | The bank gave no decision; nothing stored | Retry, ideally with the same key |

`GET /v1/payments/{id}` returns `200` with the payment, or `404` if it does not exist.

With the simulator, a card number ending in an odd digit is authorized, an even digit is
declined, and `0` makes the bank fail (`502`).

Optional headers:

- **`Idempotency-Key`**: repeating a request with the same key returns the first payment
  instead of charging again.
- **`X-Request-Id`**: echoed on the response and added to every log line; generated if absent.

## Design decisions

- **Versioned API (`/v1`).** Free now; adding it later would break every merchant's URL.
- **`201` for Declined as well as Authorized.** Either way a payment exists and can be fetched;
  a decline is the bank's business decision, not an HTTP error.
- **Bank failure is `502`, not `Declined`.** A bank error or timeout is no decision; calling it
  Declined would mislead the shopper. Nothing is stored, so a retry is safe.
- **No automatic retry of the bank call.** After a timeout we cannot know whether the card was
  charged, so a retry could charge twice. Instead the merchant retries with an
  `Idempotency-Key`, which is reserved before the bank is called, so even two concurrent
  retries reach the bank once.
- **Card data never leaks.** The full card number and CVV are never stored, logged or returned;
  only the last four digits are kept. The bank's authorization code is stored for reconciliation
  but never returned.
- **Strict input types.** Card number and CVV are strings (`012` keeps its zero), a missing
  number is rejected rather than read as 0, and `10.5` is rejected rather than truncated to 10.
- **Built test-first**, one step per commit; each skeleton flaw was fixed by the test that
  exposed it.
- **Observability:** Actuator health and HTTP metrics, a `payments` counter by status (the
  decline rate is the number worth alerting on), and a request id on every log line.

## Assumptions

- A card is valid until the end of its expiry month (UTC). Far-future years are accepted; the
  requirements set no limit.
- The amount must be above zero and fit in an `int` (up to 2,147,483,647 minor units, about
  21 million GBP); larger values are rejected. A production system would use `long`.
- "Masked card number" in the requirements is met by returning only the last four digits
  (`card_number_last_four`), as the response field tables specify.
- Only `USD`, `GBP` and `EUR` are supported (the requirements allow at most three).
- Payments are kept in memory, as the requirements allow, and are lost on restart.
- Plain HTTP is for local use only.

## What I would do next

- **Security:** HTTPS end to end, merchant authentication, card numbers in a tokenisation vault,
  Actuator on an internal port.
- **Storage:** a real database, with a unique constraint on the idempotency key, and auditing
  (`created_at`, the merchant who made the payment, and an append-only log of payment events).
- **Idempotency:** expire keys after about 24 hours, and reject a reused key sent with a
  different body.
- **Resilience:** a circuit breaker on the bank call, configurable timeouts.
- **Errors:** one standard format with stable codes (RFC 9457 Problem Details).
- **Operations:** dashboards and alerts on the metrics, distributed tracing, image scanning in CI.
- **Code:** put the repository and bank client behind interfaces once a second implementation
  exists (a database, another acquirer). Today each has one, so the interfaces were left out.
  Upgrade to Spring Boot 3.2+ to replace `RestTemplate` with `RestClient`.
  As features grow, split `model` into `api` and `domain` packages and have the service take a
  domain command instead of the HTTP request type.

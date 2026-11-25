package com.checkout.payment.gateway.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyKeys {

  private final ConcurrentHashMap<String, Optional<UUID>> keys = new ConcurrentHashMap<>();

  public boolean reserve(String key) {
    return keys.putIfAbsent(key, Optional.empty()) == null;
  }

  public Optional<UUID> paymentIdFor(String key) {
    return keys.getOrDefault(key, Optional.empty());
  }

  public void complete(String key, UUID paymentId) {
    keys.put(key, Optional.of(paymentId));
  }

  public void release(String key) {
    keys.remove(key);
  }
}

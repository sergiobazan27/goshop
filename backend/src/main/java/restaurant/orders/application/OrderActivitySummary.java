package restaurant.orders.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import restaurant.orders.domain.OrderStatus;

public record OrderActivitySummary(UUID id, String tableName, OrderStatus status,
  Instant createdAt, Instant updatedAt, long quantity, String products, BigDecimal total) {}

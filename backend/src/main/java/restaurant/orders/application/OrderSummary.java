package restaurant.orders.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import restaurant.orders.domain.OrderStatus;
import restaurant.tables.application.DiningTableSummary;

public record OrderSummary(UUID id, DiningTableSummary table, OrderStatus status, long revision,
  Instant createdAt, String openedBy, Instant closedAt, String closedBy, List<Item> items, BigDecimal total) {
  public record Item(UUID productId, String name, BigDecimal unitPrice, int quantity, BigDecimal total) {}
}
